/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code clickhouse} service.
 *
 * @author Stephane Nicoll
 */
class ClickHouseJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	protected ClickHouseJdbcDockerComposeConnectionDetailsFactory() {
		super("clickhouse/clickhouse-server");
	}

	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ClickhouseJdbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@code clickhouse}
	 * {@link RunningService}.
	 */
	static class ClickhouseJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("clickhouse", 8123);

		private final ClickHouseEnvironment environment;

		private final String jdbcUrl;

		ClickhouseJdbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ClickHouseEnvironment(service.env());
			this.jdbcUrl = jdbcUrlBuilder.build(service, urlEncode(this.environment.getDatabase()));
		}

		@Override
		public String getUsername() {
			return this.environment.getUsername();
		}

		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

		/**
		 * Percent-encodes the database name so that names with characters that are
		 * significant in a JDBC URL ('?', '%', and others) survive the ClickHouse
		 * driver's URL parsing, which percent-decodes the database segment. Unlike the
		 * PostgreSQL and MySQL drivers, the ClickHouse driver leaves a literal '+'
		 * untouched, so spaces are encoded as {@code %20} rather than the
		 * x-www-form-urlencoded {@code '+'}.
		 * @param database the database name to encode
		 * @return the encoded database name
		 */
		private static String urlEncode(String database) {
			return URLEncoder.encode(database, StandardCharsets.UTF_8).replace("+", "%20");
		}

	}

}
