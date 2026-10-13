/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code mssql/server} service.
 *
 * @author Andy Wilkinson
 */
class SqlServerJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	protected SqlServerJdbcDockerComposeConnectionDetailsFactory() {
		super("mssql/server");
	}

	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new SqlServerJdbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@code mssql/server}
	 * {@link RunningService}.
	 */
	static class SqlServerJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new SqlServerJdbcUrlBuilder("sqlserver", 1433);

		private final SqlServerEnvironment environment;

		private final String jdbcUrl;

		SqlServerJdbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new SqlServerEnvironment(service.env());
			this.jdbcUrl = disableEncryptionIfNecessary(jdbcUrlBuilder.build(service));
		}

		private String disableEncryptionIfNecessary(String jdbcUrl) {
			if (hasEncryptProperty(jdbcUrl)) {
				return jdbcUrl;
			}
			StringBuilder jdbcUrlBuilder = new StringBuilder(jdbcUrl);
			if (!jdbcUrl.endsWith(";")) {
				jdbcUrlBuilder.append(";");
			}
			jdbcUrlBuilder.append("encrypt=false");
			return jdbcUrlBuilder.toString();
		}

		private boolean hasEncryptProperty(String jdbcUrl) {
			for (String property : jdbcUrl.split(";")) {
				if (property.startsWith("encrypt=")) {
					return true;
				}
			}
			return false;
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

		private static final class SqlServerJdbcUrlBuilder extends JdbcUrlBuilder {

			private SqlServerJdbcUrlBuilder(String driverProtocol, int containerPort) {
				super(driverProtocol, containerPort);
			}

			@Override
			protected void appendParameters(StringBuilder url, String parameters) {
				url.append(";").append(parameters);
			}

		}

	}

}
