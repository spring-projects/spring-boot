/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.mongo;

import com.mongodb.ConnectionString;

import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link MongoConnectionDetails}
 * for a {@code mongo} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MongoDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<MongoConnectionDetails> {

	private static final int MONGODB_PORT = 27017;

	protected MongoDockerComposeConnectionDetailsFactory() {
		super("mongo", "com.mongodb.ConnectionString");
	}

	@Override
	protected MongoDockerComposeConnectionDetails getDockerComposeConnectionDetails(
			DockerComposeConnectionSource source) {
		return new MongoDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link MongoConnectionDetails} backed by a {@code mongo} {@link RunningService}.
	 */
	static class MongoDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements MongoConnectionDetails {

		private final ConnectionString connectionString;

		MongoDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.connectionString = buildConnectionString(service);

		}

		private ConnectionString buildConnectionString(RunningService service) {
			MongoEnvironment environment = new MongoEnvironment(service.env());
			StringBuilder builder = new StringBuilder("mongodb://");
			if (environment.getUsername() != null) {
				builder.append(environment.getUsername());
				builder.append(":");
				builder.append((environment.getPassword() != null) ? environment.getPassword() : "");
				builder.append("@");
			}
			builder.append(service.host());
			builder.append(":");
			builder.append(service.ports().get(MONGODB_PORT));
			builder.append("/");
			builder.append((environment.getDatabase() != null) ? environment.getDatabase() : "test");
			if (environment.getUsername() != null) {
				builder.append("?authSource=admin");
			}
			return new ConnectionString(builder.toString());
		}

		@Override
		public ConnectionString getConnectionString() {
			return this.connectionString;
		}

	}

}
