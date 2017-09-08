/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mongo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mongodb.ConnectionString;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClientSettings.Builder;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;
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
		this.builderCustomizers = (builderCustomizers != null ? builderCustomizers
				: Collections.emptyList());
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
		String host = this.properties.getHost() == null ? "localhost"
				: this.properties.getHost();
		ClusterSettings clusterSettings = ClusterSettings.builder()
				.hosts(Collections.singletonList(new ServerAddress(host, port))).build();
		builder.clusterSettings(clusterSettings);
		return MongoClients.create(builder.build());
	}

	private MongoClient createNetworkMongoClient(MongoClientSettings settings) {
		if (hasCustomAddress() || hasCustomCredentials()) {
			return createCredentialNetworkMongoClient(settings);
		}
		ConnectionString connectionString = new ConnectionString(
				this.properties.determineUri());
		return MongoClients.create(createBuilder(settings, connectionString).build());
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
		builder.clusterSettings(ClusterSettings.builder()
				.hosts(Collections.singletonList(serverAddress)).build());
		return MongoClients.create(builder.build());
	}

	private void applyCredentials(Builder builder) {
		List<MongoCredential> credentials = new ArrayList<>();
		String database = this.properties.getAuthenticationDatabase() == null
				? this.properties.getMongoClientDatabase()
				: this.properties.getAuthenticationDatabase();
		credentials.add(MongoCredential.createCredential(this.properties.getUsername(),
				database, this.properties.getPassword()));
		builder.credentialList(credentials);
	}

	private <T> T getOrDefault(T value, T defaultValue) {
		return (value == null ? defaultValue : value);
	}

	private Builder createBuilder(MongoClientSettings settings,
			ConnectionString connection) {
		Builder builder = builder(settings);
		builder.clusterSettings(getClusterSettings(connection));
		builder.connectionPoolSettings(getConnectionPoolSettings(connection));
		builder.serverSettings(getServerSettings(connection));
		builder.credentialList(connection.getCredentialList());
		builder.sslSettings(getSslSettings(connection));
		builder.socketSettings(getSocketSettings(connection));
		if (connection.getReadPreference() != null) {
			builder.readPreference(connection.getReadPreference());
		}
		if (connection.getReadConcern() != null) {
			builder.readConcern(connection.getReadConcern());
		}
		if (connection.getWriteConcern() != null) {
			builder.writeConcern(connection.getWriteConcern());
		}
		if (connection.getApplicationName() != null) {
			builder.applicationName(connection.getApplicationName());
		}
		customize(builder);
		return builder;
	}

	private ClusterSettings getClusterSettings(ConnectionString connection) {
		return ClusterSettings.builder().applyConnectionString(connection).build();
	}

	private ConnectionPoolSettings getConnectionPoolSettings(
			ConnectionString connection) {
		return ConnectionPoolSettings.builder().applyConnectionString(connection).build();
	}

	private ServerSettings getServerSettings(ConnectionString connection) {
		return ServerSettings.builder().applyConnectionString(connection).build();
	}

	private SslSettings getSslSettings(ConnectionString connection) {
		return SslSettings.builder().applyConnectionString(connection).build();
	}

	private SocketSettings getSocketSettings(ConnectionString connection) {
		return SocketSettings.builder().applyConnectionString(connection).build();
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
