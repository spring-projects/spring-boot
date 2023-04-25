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

package org.springframework.boot.docker.compose.service.connection.jdbc;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility used to build a JDBC URL for a {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class JdbcUrlBuilder {

	private static final String PARAMETERS_LABEL = "org.springframework.boot.jdbc.parameters";

	private final String driverProtocol;

	private final int containerPort;

	/**
	 * Create a new {@link JdbcUrlBuilder} instance.
	 * @param driverProtocol the driver protocol
	 * @param containerPort the source container port
	 */
	public JdbcUrlBuilder(String driverProtocol, int containerPort) {
		Assert.notNull(driverProtocol, "DriverProtocol must not be null");
		this.driverProtocol = driverProtocol;
		this.containerPort = containerPort;
	}

	/**
	 * Build a JDBC URL for the given {@link RunningService}.
	 * @param service the running service
	 * @return a new JDBC URL
	 */
	public String build(RunningService service) {
		return build(service, null);
	}

	/**
	 * Build a JDBC URL for the given {@link RunningService} and database.
	 * @param service the running service
	 * @param database the database to connect to
	 * @return a new JDBC URL
	 */
	public String build(RunningService service, String database) {
		return urlFor(service, database);
	}

	private String urlFor(RunningService service, String database) {
		Assert.notNull(service, "Service must not be null");
		String parameters = getParameters(service);
		StringBuilder url = new StringBuilder("jdbc:%s://%s:%d".formatted(this.driverProtocol, service.host(),
				service.ports().get(this.containerPort)));
		if (StringUtils.hasLength(database)) {
			url.append("/");
			url.append(database);
		}
		url.append(parameters);
		return url.toString();
	}

	private String getParameters(RunningService service) {
		String parameters = service.labels().get(PARAMETERS_LABEL);
		return (StringUtils.hasLength(parameters)) ? "?" + parameters : "";
	}

}
