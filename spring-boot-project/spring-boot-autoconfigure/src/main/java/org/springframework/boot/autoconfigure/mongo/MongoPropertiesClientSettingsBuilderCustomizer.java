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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * A {@link MongoClientSettingsBuilderCustomizer} that applies properties from a
 * {@link MongoProperties} to a {@link MongoClientSettings}.
 *
 * @author Scott Frederick
 * @since 2.4.0
 */
public class MongoPropertiesClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer, Ordered {

	private final MongoProperties properties;

	private final Environment environment;

	private int order = 0;

	public MongoPropertiesClientSettingsBuilderCustomizer(MongoProperties properties, Environment environment) {
		this.properties = properties;
		this.environment = environment;
	}

	@Override
	public void customize(MongoClientSettings.Builder settingsBuilder) {
		validateConfiguration();
		applyUuidRepresentation(settingsBuilder);
		applyHostAndPort(settingsBuilder);
		applyCredentials(settingsBuilder);
		applyReplicaSet(settingsBuilder);
	}

	private void validateConfiguration() {
		if (hasCustomAddress() || hasCustomCredentials() || hasReplicaSet()) {
			Assert.state(this.properties.getUri() == null,
					"Invalid mongo configuration, either uri or host/port/credentials/replicaSet must be specified");
		}
	}

	private void applyUuidRepresentation(MongoClientSettings.Builder settingsBuilder) {
		settingsBuilder.uuidRepresentation(this.properties.getUuidRepresentation());
	}

	private void applyHostAndPort(MongoClientSettings.Builder settings) {
		if (getEmbeddedPort() != null) {
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

	private void applyCredentials(MongoClientSettings.Builder builder) {
		if (hasCustomCredentials()) {
			String database = (this.properties.getAuthenticationDatabase() != null)
					? this.properties.getAuthenticationDatabase() : this.properties.getMongoClientDatabase();
			builder.credential((MongoCredential.createCredential(this.properties.getUsername(), database,
					this.properties.getPassword())));
		}
	}

	private void applyReplicaSet(MongoClientSettings.Builder builder) {
		if (hasReplicaSet()) {
			builder.applyToClusterSettings(
					(cluster) -> cluster.requiredReplicaSetName(this.properties.getReplicaSetName()));
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

	private boolean hasCustomCredentials() {
		return this.properties.getUsername() != null && this.properties.getPassword() != null;
	}

	private boolean hasCustomAddress() {
		return this.properties.getHost() != null || this.properties.getPort() != null;
	}

	private boolean hasReplicaSet() {
		return this.properties.getReplicaSetName() != null;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order value of this object.
	 * @param order the new order value
	 * @see #getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
