/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mongo;

import java.util.Collections;
import java.util.List;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.springframework.core.env.Environment;

/**
 * A factory for a blocking {@link MongoClient} that applies {@link MongoProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Josh Long
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Nasko Vasilev
 * @author Mark Paluch
 * @since 2.0.0
 */
public class MongoClientFactory {

	private final MongoProperties properties;

	private final Environment environment;

	public MongoClientFactory(MongoProperties properties, Environment environment) {
		this.properties = properties;
		this.environment = environment;
	}

	/**
	 * Creates a {@link MongoClient} using the given {@link MongoClientSettings settings}.
	 * If the environment contains a {@code local.mongo.port} property, it is used to
	 * configure a client to an embedded MongoDB instance.
	 * @param settings the settings
	 * @return the Mongo client
	 */
	public MongoClient createMongoClient(MongoClientSettings settings) {
		Builder settingsBuilder = (settings != null) ? MongoClientSettings.builder(settings)
				: MongoClientSettings.builder();
		settingsBuilder.uuidRepresentation(this.properties.getUuidRepresentation());
		Integer embeddedPort = getEmbeddedPort();
		if (embeddedPort != null) {
			return createEmbeddedMongoClient(settingsBuilder, embeddedPort);
		}
		return createNetworkMongoClient(settingsBuilder);
	}

	private Integer getEmbeddedPort() {
		if (this.environment != null) {
			String localPort = this.environment.getProperty("local.mongo.port");
			if (localPort != null) {
				return Integer.valueOf(localPort);
			}
		}
		return null;
	}

	private MongoClient createEmbeddedMongoClient(Builder settings, int port) {
		String host = (this.properties.getHost() != null) ? this.properties.getHost() : "localhost";
		settings.applyToClusterSettings(
				(cluster) -> cluster.hosts(Collections.singletonList(new ServerAddress(host, port))));
		return MongoClients.create(settings.build());
	}

	private MongoClient createNetworkMongoClient(Builder settings) {
		MongoProperties properties = this.properties;
		if (properties.getUri() != null) {
			return createMongoClient(properties.getUri(), settings);
		}
		if (hasCustomAddress() || hasCustomCredentials()) {
			if (hasCustomCredentials()) {
				String database = (this.properties.getAuthenticationDatabase() != null)
						? this.properties.getAuthenticationDatabase() : this.properties.getMongoClientDatabase();
				settings.credential((MongoCredential.createCredential(this.properties.getUsername(), database,
						this.properties.getPassword())));
			}
			String host = getValue(properties.getHost(), "localhost");
			int port = getValue(properties.getPort(), MongoProperties.DEFAULT_PORT);
			List<ServerAddress> seeds = Collections.singletonList(new ServerAddress(host, port));
			settings.applyToClusterSettings((cluster) -> cluster.hosts(seeds));
			return MongoClients.create(settings.build());
		}
		return createMongoClient(MongoProperties.DEFAULT_URI, settings);
	}

	private MongoClient createMongoClient(String uri, Builder settings) {
		settings.applyConnectionString(new ConnectionString(uri));
		return MongoClients.create(settings.build());
	}

	private <T> T getValue(T value, T fallback) {
		return (value != null) ? value : fallback;
	}

	private boolean hasCustomAddress() {
		return this.properties.getHost() != null || this.properties.getPort() != null;
	}

	private boolean hasCustomCredentials() {
		return this.properties.getUsername() != null && this.properties.getPassword() != null;
	}

}
