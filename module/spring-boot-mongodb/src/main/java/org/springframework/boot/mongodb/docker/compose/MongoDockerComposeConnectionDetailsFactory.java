/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.docker.compose;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.mongodb.ConnectionString;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

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

		private final @Nullable SslBundle sslBundle;

		MongoDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.sslBundle = getSslBundle(service);
			this.connectionString = buildConnectionString(service);

		}

		private ConnectionString buildConnectionString(RunningService service) {
			MongoEnvironment environment = new MongoEnvironment(service.env());
			String username = environment.getUsername();
			String password = environment.getPassword();
			String database = environment.getDatabase();
			StringBuilder builder = new StringBuilder("mongodb://");
			if (username != null) {
				builder.append(encode(username));
				builder.append(":");
				builder.append((password != null) ? encode(password) : "");
				builder.append("@");
			}
			builder.append(service.host());
			builder.append(":");
			builder.append(service.ports().getTcp(MONGODB_PORT));
			builder.append("/");
			builder.append((database != null) ? encode(database) : "test");
			builder.append((username != null) ? "?authSource=admin" : "");
			return new ConnectionString(builder.toString());
		}

		private static String encode(String input) {
			return URLEncoder.encode(input, StandardCharsets.UTF_8).replace("+", "%20");
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

		@Override
		public ConnectionString getConnectionString() {
			return this.connectionString;
		}

	}

}
