/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.docker.compose;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code postgres} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PostgresJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	protected PostgresJdbcDockerComposeConnectionDetailsFactory() {
		super("postgres");
	}

	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new PostgresJdbcDockerComposeConnectionDetails(source.getRunningService(), source.getEnvironment());
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@code postgres} {@link RunningService}.
	 */
	static class PostgresJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("postgresql", 5432);

		private final PostgresEnvironment environment;

		private final String jdbcUrl;

		PostgresJdbcDockerComposeConnectionDetails(RunningService service, Environment environment) {
			super(service);
			this.environment = new PostgresEnvironment(service.env());
			this.jdbcUrl = addApplicationNameIfNecessary(jdbcUrlBuilder.build(service, this.environment.getDatabase()),
					environment);
		}

		@Override
		public String getUsername() {
			return this.environment.getUsername();
		}

		@Override
		public @Nullable String getPassword() {
			return this.environment.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

		private static String addApplicationNameIfNecessary(String jdbcUrl, Environment environment) {
			if (jdbcUrl.contains("&ApplicationName=") || jdbcUrl.contains("?ApplicationName=")) {
				return jdbcUrl;
			}
			String applicationName = environment.getProperty("spring.application.name");
			if (!StringUtils.hasText(applicationName)) {
				return jdbcUrl;
			}
			StringBuilder jdbcUrlBuilder = new StringBuilder(jdbcUrl);
			if (!jdbcUrl.contains("?")) {
				jdbcUrlBuilder.append("?");
			}
			else if (!jdbcUrl.endsWith("&")) {
				jdbcUrlBuilder.append("&");
			}
			return jdbcUrlBuilder.append("ApplicationName")
				.append('=')
				.append(URLEncoder.encode(applicationName, StandardCharsets.UTF_8))
				.toString();
		}

	}

}
