/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.test.web;

import java.util.Objects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * {@link EnvironmentPostProcessor} implementation to start the management context on a
 * random port if the main server's port is 0 and the management context is expected on a
 * different port.
 *
 * @author Madhura Bhave
 * @since 2.1.0
 */
public class SpringBootTestRandomPortEnvironmentPostProcessor
		implements EnvironmentPostProcessor {

	private static final String MANAGEMENT_PORT_PROPERTY = "management.server.port";

	private static final String SERVER_PORT_PROPERTY = "server.port";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		MapPropertySource source = (MapPropertySource) environment.getPropertySources()
				.get(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
		if (isTestServerPortRandom(source)) {
			if (source.getProperty(MANAGEMENT_PORT_PROPERTY) == null) {
				String managementPort = getPort(environment, MANAGEMENT_PORT_PROPERTY,
						null);
				String serverPort = getPort(environment, SERVER_PORT_PROPERTY, "8080");
				if (managementPort != null && !managementPort.equals("-1")) {
					if (!managementPort.equals(serverPort)) {
						source.getSource().put(MANAGEMENT_PORT_PROPERTY, "0");
					}
					else {
						source.getSource().put(MANAGEMENT_PORT_PROPERTY, "");
					}
				}
			}
		}

	}

	private boolean isTestServerPortRandom(MapPropertySource source) {
		return (source != null && "0".equals(source.getProperty(SERVER_PORT_PROPERTY)));
	}

	private String getPort(ConfigurableEnvironment environment, String property,
			String defaultValue) {
		return environment.getPropertySources().stream()
				.filter((source) -> !source.getName().equals(
						TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME))
				.map((source) -> (String) source.getProperty(property))
				.filter(Objects::nonNull).findFirst().orElse(defaultValue);
	}

}
