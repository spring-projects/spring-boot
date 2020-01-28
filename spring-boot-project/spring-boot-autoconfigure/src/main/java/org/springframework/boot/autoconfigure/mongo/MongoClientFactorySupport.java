/*
 * Copyright 2019 the original author or authors.
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
import com.mongodb.MongoDriverInformation;
import com.mongodb.ServerAddress;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Base class for common setup bits (aka {@link MongoClientSettings}) required for
 * instantiating a MongoClient.
 *
 * @author Christoph Strobl
 * @since 2.3.0
 */
public abstract class MongoClientFactorySupport<T> {

	private final MongoProperties properties;

	private final Environment environment;

	private final List<MongoClientSettingsBuilderCustomizer> builderCustomizers;

	public MongoClientFactorySupport(MongoProperties properties, Environment environment,
			List<MongoClientSettingsBuilderCustomizer> builderCustomizers) {
		this.properties = properties;
		this.environment = environment;
		this.builderCustomizers = (builderCustomizers != null) ? builderCustomizers : Collections.emptyList();
	}

	public T createMongoClient(MongoClientSettings settings) {
		MongoClientSettings targetSettings = computeClientSettings(settings);
		return (getEmbeddedPort() != null) ? createEmbeddedMongoClient(targetSettings)
				: createNetworkMongoClient(targetSettings);
	}

	private MongoClientSettings computeClientSettings(MongoClientSettings settings) {

		Builder settingsBuilder = (settings != null) ? MongoClientSettings.builder(settings)
				: MongoClientSettings.builder();
		applyHostAndPort(settingsBuilder);
		applyCredentials(settingsBuilder);

		customize(settingsBuilder);
		return settingsBuilder.build();
	}

	private void applyHostAndPort(MongoClientSettings.Builder settings) {

		if (isEmbedded()) {
			settings.applyConnectionString(new ConnectionString("mongodb://localhost:" + getEmbeddedPort()));
			return;
		}
		if (!this.properties.determineUri().equals(MongoProperties.DEFAULT_URI)) {
			if (hasCustomAddress()) {
				Assert.state(this.properties.getUri() == null,
						"Invalid mongo configuration, either uri or host/port/credentials must be specified");
			}
			settings.applyConnectionString(new ConnectionString(this.properties.determineUri()));
		}
		else if (hasCustomAddress()) {
			String host = getOrDefault(this.properties.getHost(), "localhost");
			int port = getOrDefault(this.properties.getPort(), MongoProperties.DEFAULT_PORT);

			ServerAddress serverAddress = new ServerAddress(host, port);

			settings.applyToClusterSettings((cluster) -> cluster.hosts(Collections.singletonList(serverAddress)));
		}
	}

	private void applyCredentials(Builder builder) {

		if (hasCustomCredentials()) {
			String database = (this.properties.getAuthenticationDatabase() != null)
					? this.properties.getAuthenticationDatabase() : this.properties.getMongoClientDatabase();
			builder.credential((MongoCredential.createCredential(this.properties.getUsername(), database,
					this.properties.getPassword())));
		}
	}

	private void customize(MongoClientSettings.Builder builder) {
		for (MongoClientSettingsBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
	}

	private <T> T getOrDefault(T value, T defaultValue) {
		return (value != null) ? value : defaultValue;
	}

	protected abstract T createNetworkMongoClient(MongoClientSettings settings);

	protected abstract T createEmbeddedMongoClient(MongoClientSettings settings);

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

	private boolean hasCustomAddress() {
		return this.properties.getHost() != null || this.properties.getPort() != null;
	}

	protected static MongoDriverInformation driverInformation() {
		return MongoDriverInformation.builder(MongoDriverInformation.builder().build()).driverName("spring-boot")
				.build();
	}

}
