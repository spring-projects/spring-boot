/*
 * Copyright 2012-2018 the original author or authors.
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
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * A factory for a reactive {@link MongoClient} that applies {@link MongoProperties}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ReactiveMongoClientFactory {

	private final MongoProperties properties;

	private final Environment environment;

	private final List<MongoClientSettingsBuilderCustomizer> builderCustomizers;

	public ReactiveMongoClientFactory(MongoProperties properties, Environment environment,
			List<MongoClientSettingsBuilderCustomizer> builderCustomizers) {
		this.properties = properties;
		this.environment = environment;
		this.builderCustomizers = (builderCustomizers != null) ? builderCustomizers
				: Collections.emptyList();
	}

	/**
	 * Creates a {@link MongoClient} using the given {@code settings}. If the environment
	 * contains a {@code local.mongo.port} property, it is used to configure a client to
	 * an embedded MongoDB instance.
	 * @param settings the settings
	 * @return the Mongo client
	 */
	public MongoClient createMongoClient(MongoClientSettings settings) {
		Integer embeddedPort = getEmbeddedPort();
		if (embeddedPort != null) {
			return createEmbeddedMongoClient(settings, embeddedPort);
		}
		return createNetworkMongoClient(settings);
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

	private MongoClient createEmbeddedMongoClient(MongoClientSettings settings,
			int port) {
		Builder builder = builder(settings);
		String host = (this.properties.getHost() != null) ? this.properties.getHost()
				: "localhost";
		builder.applyToClusterSettings((cluster) -> cluster
				.hosts(Collections.singletonList(new ServerAddress(host, port))));
		return createMongoClient(builder);
	}

	private MongoClient createNetworkMongoClient(MongoClientSettings settings) {
		if (hasCustomAddress() || hasCustomCredentials()) {
			return createCredentialNetworkMongoClient(settings);
		}
		ConnectionString connectionString = new ConnectionString(
				this.properties.determineUri());
		return createMongoClient(createBuilder(settings, connectionString));
	}

	private MongoClient createCredentialNetworkMongoClient(MongoClientSettings settings) {
		Assert.state(this.properties.getUri() == null, "Invalid mongo configuration, "
				+ "either uri or host/port/credentials must be specified");
		Builder builder = builder(settings);
		if (hasCustomCredentials()) {
			applyCredentials(builder);
		}
		String host = getOrDefault(this.properties.getHost(), "localhost");
		int port = getOrDefault(this.properties.getPort(), MongoProperties.DEFAULT_PORT);
		ServerAddress serverAddress = new ServerAddress(host, port);
		builder.applyToClusterSettings(
				(cluster) -> cluster.hosts(Collections.singletonList(serverAddress)));
		return createMongoClient(builder);
	}

	private void applyCredentials(Builder builder) {
		String database = (this.properties.getAuthenticationDatabase() != null)
				? this.properties.getAuthenticationDatabase()
				: this.properties.getMongoClientDatabase();
		builder.credential((MongoCredential.createCredential(
				this.properties.getUsername(), database, this.properties.getPassword())));
	}

	private <T> T getOrDefault(T value, T defaultValue) {
		return (value != null) ? value : defaultValue;
	}

	private MongoClient createMongoClient(Builder builder) {
		customize(builder);
		return MongoClients.create(builder.build());
	}

	private Builder createBuilder(MongoClientSettings settings,
			ConnectionString connection) {
		return builder(settings).applyConnectionString(connection);
	}

	private void customize(MongoClientSettings.Builder builder) {
		for (MongoClientSettingsBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
	}

	private boolean hasCustomAddress() {
		return this.properties.getHost() != null || this.properties.getPort() != null;
	}

	private boolean hasCustomCredentials() {
		return this.properties.getUsername() != null
				&& this.properties.getPassword() != null;
	}

	private Builder builder(MongoClientSettings settings) {
		if (settings == null) {
			return MongoClientSettings.builder();
		}
		return MongoClientSettings.builder(settings);
	}

}
