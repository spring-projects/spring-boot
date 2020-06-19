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
import java.util.function.BiFunction;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.MongoDriverInformation;
import com.mongodb.ServerAddress;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Base class for setup that is common to MongoDB client factories.
 *
 * @param <T> the mongo client type
 * @author Christoph Strobl
 * @author Scott Frederick
 * @since 2.3.0
 */
public abstract class MongoClientFactorySupport<T> {

	private final MongoProperties properties;

	private final Environment environment;

	private final List<MongoClientSettingsBuilderCustomizer> builderCustomizers;

	private final BiFunction<MongoClientSettings, MongoDriverInformation, T> clientCreator;

	protected MongoClientFactorySupport(MongoProperties properties, Environment environment,
			List<MongoClientSettingsBuilderCustomizer> builderCustomizers,
			BiFunction<MongoClientSettings, MongoDriverInformation, T> clientCreator) {
		this.properties = properties;
		this.environment = environment;
		this.builderCustomizers = (builderCustomizers != null) ? builderCustomizers : Collections.emptyList();
		this.clientCreator = clientCreator;
	}

	public T createMongoClient(MongoClientSettings settings) {
		MongoClientSettings targetSettings = computeClientSettings(settings);
		return this.clientCreator.apply(targetSettings, driverInformation());
	}

	private MongoClientSettings computeClientSettings(MongoClientSettings settings) {
		Builder settingsBuilder = (settings != null) ? MongoClientSettings.builder(settings)
				: MongoClientSettings.builder();
		validateConfiguration();
		applyUuidRepresentation(settingsBuilder);
		applyHostAndPort(settingsBuilder);
		applyCredentials(settingsBuilder);
		applyReplicaSet(settingsBuilder);
		customize(settingsBuilder);
		return settingsBuilder.build();
	}

	private void validateConfiguration() {
		if (hasCustomAddress() || hasCustomCredentials() || hasReplicaSet()) {
			Assert.state(this.properties.getUri() == null,
					"Invalid mongo configuration, either uri or host/port/credentials/replicaSet must be specified");
		}
	}

	private void applyUuidRepresentation(Builder settingsBuilder) {
		settingsBuilder.uuidRepresentation(this.properties.getUuidRepresentation());
	}

	private void applyHostAndPort(MongoClientSettings.Builder settings) {
		if (isEmbedded()) {
			settings.applyConnectionString(new ConnectionString("mongodb://localhost:" + getEmbeddedPort()));
			return;
		}

		if (hasCustomAddress()) {
			String host = getOrDefault(this.properties.getHost(), "localhost");
			int port = getOrDefault(this.properties.getPort(), MongoProperties.DEFAULT_PORT);
			ServerAddress serverAddress = new ServerAddress(host, port);
			settings.applyToClusterSettings((cluster) -> cluster.hosts(Collections.singletonList(serverAddress)));
			return;
		}

		settings.applyConnectionString(new ConnectionString(this.properties.determineUri()));
	}

	private void applyCredentials(Builder builder) {
		if (hasCustomCredentials()) {
			String database = (this.properties.getAuthenticationDatabase() != null)
					? this.properties.getAuthenticationDatabase() : this.properties.getMongoClientDatabase();
			builder.credential((MongoCredential.createCredential(this.properties.getUsername(), database,
					this.properties.getPassword())));
		}
	}

	private void applyReplicaSet(Builder builder) {
		if (hasReplicaSet()) {
			builder.applyToClusterSettings(
					(cluster) -> cluster.requiredReplicaSetName(this.properties.getReplicaSetName()));
		}
	}

	private void customize(MongoClientSettings.Builder builder) {
		for (MongoClientSettingsBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
	}

	private <V> V getOrDefault(V value, V defaultValue) {
		return (value != null) ? value : defaultValue;
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

	private boolean isEmbedded() {
		return getEmbeddedPort() != null;
	}

	private boolean hasCustomCredentials() {
		return this.properties.getUsername() != null && this.properties.getPassword() != null;
	}

	private boolean hasReplicaSet() {
		return this.properties.getReplicaSetName() != null;
	}

	private boolean hasCustomAddress() {
		return this.properties.getHost() != null || this.properties.getPort() != null;
	}

	private MongoDriverInformation driverInformation() {
		return MongoDriverInformation.builder(MongoDriverInformation.builder().build()).driverName("spring-boot")
				.build();
	}

}
