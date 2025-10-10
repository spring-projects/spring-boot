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

package org.springframework.boot.r2dbc.docker.compose;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility used to build an R2DBC {@link ConnectionFactoryOptions} for a
 * {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConnectionFactoryOptionsBuilder {

	private static final String PARAMETERS_LABEL = "org.springframework.boot.r2dbc.parameters";

	private final String driver;

	private final int sourcePort;

	/**
	 * Create a new {@link ConnectionFactoryOptionsBuilder} instance.
	 * @param driver the driver
	 * @param containerPort the source container port
	 */
	ConnectionFactoryOptionsBuilder(String driver, int containerPort) {
		Assert.notNull(driver, "'driver' must not be null");
		this.driver = driver;
		this.sourcePort = containerPort;
	}

	ConnectionFactoryOptions build(RunningService service, String database, @Nullable String user,
			@Nullable String password) {
		Assert.notNull(service, "'service' must not be null");
		Assert.notNull(database, "'database' must not be null");
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
			Assert.state(parts.length == 2, () -> "'parameters' [%s] must contain parsable value".formatted(parameter));
			result.put(parts[0], parts[1]);
		}
		return Collections.unmodifiableMap(result);
	}

}
