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

package org.springframework.boot.docker.compose.service.connection.r2dbc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.jdbc.JdbcUrlBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility used to build an R2DBC {@link ConnectionFactoryOptions} for a
 * {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class ConnectionFactoryOptionsBuilder {

	private static final String PARAMETERS_LABEL = "org.springframework.boot.r2dbc.parameters";

	private final String driver;

	private final int sourcePort;

	/**
	 * Create a new {@link JdbcUrlBuilder} instance.
	 * @param driver the driver protocol
	 * @param containerPort the source container port
	 */
	public ConnectionFactoryOptionsBuilder(String driver, int containerPort) {
		Assert.notNull(driver, "Driver must not be null");
		this.driver = driver;
		this.sourcePort = containerPort;
	}

	public ConnectionFactoryOptions build(RunningService service, String database, String user, String password) {
		Assert.notNull(service, "Service must not be null");
		Assert.notNull(database, "Database must not be null");
		ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions.builder()
			.option(ConnectionFactoryOptions.DRIVER, this.driver)
			.option(ConnectionFactoryOptions.HOST, service.host())
			.option(ConnectionFactoryOptions.PORT, service.ports().get(this.sourcePort))
			.option(ConnectionFactoryOptions.DATABASE, database);
		if (StringUtils.hasLength(user)) {
			builder.option(ConnectionFactoryOptions.USER, user);
		}
		if (StringUtils.hasLength(password)) {
			builder.option(ConnectionFactoryOptions.PASSWORD, password);
		}
		applyParameters(service, builder);
		return builder.build();
	}

	private void applyParameters(RunningService service, ConnectionFactoryOptions.Builder builder) {
		String parameters = service.labels().get(PARAMETERS_LABEL);
		try {
			if (StringUtils.hasText(parameters)) {
				parseParameters(parameters).forEach((name, value) -> builder.option(Option.valueOf(name), value));
			}
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException(
					"Unable to apply R2DBC label parameters '%s' defined on service %s".formatted(parameters, service));
		}
	}

	private Map<String, String> parseParameters(String parameters) {
		Map<String, String> result = new LinkedHashMap<>();
		for (String parameter : StringUtils.commaDelimitedListToStringArray(parameters)) {
			String[] parts = parameter.split("=");
			Assert.state(parts.length == 2, () -> "Unable to parse parameter '%s'".formatted(parameter));
			result.put(parts[0], parts[1]);
		}
		return Collections.unmodifiableMap(result);
	}

}
