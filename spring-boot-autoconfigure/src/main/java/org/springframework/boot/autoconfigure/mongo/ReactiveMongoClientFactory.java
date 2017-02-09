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

/**
 * A factory for a reactive {@link MongoClient} that applies {@link MongoProperties}.
 *
 * @author Mark Paluch
 * @since 2.0.0
 */
public class ReactiveMongoClientFactory {

	private final MongoProperties properties;

	public ReactiveMongoClientFactory(MongoProperties properties) {
		this.properties = properties;
	}

	/**
	 * Creates a {@link MongoClient} using the given {@code options} and
	 * {@code environment}. If the configured port is zero, the value of the
	 * {@code local.mongo.port} property retrieved from the {@code environment} is used to
	 * configure the client.
	 * @param settings the settings
	 * @param environment the environment
	 * @return the Mongo client
	 */
	public MongoClient createMongoClient(MongoClientSettings settings,
			Environment environment) {
		if (hasCustomAddress() || hasCustomCredentials()) {
			if (this.properties.getUri() != null) {
				throw new IllegalStateException("Invalid mongo configuration, "
						+ "either uri or host/port/credentials must be specified");
			}

			Builder builder = builder(settings);
			if (hasCustomCredentials()) {
				List<MongoCredential> credentials = new ArrayList<MongoCredential>();
				String database = this.properties.getAuthenticationDatabase() == null ? this.properties
						.getMongoClientDatabase() : this.properties
						.getAuthenticationDatabase();
				credentials.add(MongoCredential.createCredential(
						this.properties.getUsername(), database,
						this.properties.getPassword()));
				builder.credentialList(credentials);
			}
			String host = this.properties.getHost() == null ? "localhost"
					: this.properties.getHost();
			int port = determinePort(environment);
			ClusterSettings clusterSettings = ClusterSettings.builder()
					.hosts(Collections.singletonList(new ServerAddress(host, port)))
					.build();
			builder.clusterSettings(clusterSettings);
			return MongoClients.create(builder.build());
		}
		ConnectionString connectionString = new ConnectionString(
				this.properties.determineUri());
		return MongoClients.create(createBuilder(settings, connectionString).build());
	}

	private Builder createBuilder(MongoClientSettings settings,
			ConnectionString connectionString) {
		Builder builder = builder(settings)
				.clusterSettings(
						ClusterSettings.builder().applyConnectionString(connectionString)
								.build())
				.connectionPoolSettings(
						ConnectionPoolSettings.builder()
								.applyConnectionString(connectionString).build())
				.serverSettings(
						ServerSettings.builder().applyConnectionString(connectionString)
								.build())
				.credentialList(connectionString.getCredentialList())
				.sslSettings(
						SslSettings.builder().applyConnectionString(connectionString)
								.build())
				.socketSettings(
						SocketSettings.builder().applyConnectionString(connectionString)
								.build());
		if (connectionString.getReadPreference() != null) {
			builder.readPreference(connectionString.getReadPreference());
		}
		if (connectionString.getReadConcern() != null) {
			builder.readConcern(connectionString.getReadConcern());
		}
		if (connectionString.getWriteConcern() != null) {
			builder.writeConcern(connectionString.getWriteConcern());
		}
		if (connectionString.getApplicationName() != null) {
			builder.applicationName(connectionString.getApplicationName());
		}
		return builder;
	}

	private boolean hasCustomAddress() {
		return this.properties.getHost() != null || this.properties.getPort() != null;
	}

	private boolean hasCustomCredentials() {
		return this.properties.getUsername() != null
				&& this.properties.getPassword() != null;
	}

	private int determinePort(Environment environment) {
		if (this.properties.getPort() == null) {
			return MongoProperties.DEFAULT_PORT;
		}
		if (this.properties.getPort() == 0) {
			if (environment != null) {
				String localPort = environment.getProperty("local.mongo.port");
				if (localPort != null) {
					return Integer.valueOf(localPort);
				}
			}
			throw new IllegalStateException(
					"spring.data.mongodb.port=0 and no local mongo port configuration "
							+ "is available");
		}
		return this.properties.getPort();
	}

	private Builder builder(MongoClientSettings settings) {
		if (settings == null) {
			return MongoClientSettings.builder();
		}

		return MongoClientSettings.builder(settings);
	}

}
