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

package org.springframework.boot.test.web;

import java.util.Objects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} implementation to start the management context on a
 * random port if the main server's port is 0 and the management context is expected on a
 * different port.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class SpringBootTestRandomPortEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String MANAGEMENT_PORT_PROPERTY = "management.server.port";

	private static final String SERVER_PORT_PROPERTY = "server.port";

	/**
     * This method is used to post-process the environment and application for the Spring Boot test.
     * It checks if the test server port is fixed or if the test management port is already configured.
     * If not, it sets the management port to 0 if it is not already set to -1 or 0.
     * If the management port is the same as the server port, it sets the management port to an empty string.
     * 
     * @param environment The configurable environment for the Spring Boot test.
     * @param application The Spring application for the test.
     */
    @Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		MapPropertySource source = (MapPropertySource) environment.getPropertySources()
			.get(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
		if (source == null || isTestServerPortFixed(source, environment) || isTestManagementPortConfigured(source)) {
			return;
		}
		Integer managementPort = getPropertyAsInteger(environment, MANAGEMENT_PORT_PROPERTY, null);
		if (managementPort == null || managementPort.equals(-1) || managementPort.equals(0)) {
			return;
		}
		Integer serverPort = getPropertyAsInteger(environment, SERVER_PORT_PROPERTY, 8080);
		if (!managementPort.equals(serverPort)) {
			source.getSource().put(MANAGEMENT_PORT_PROPERTY, "0");
		}
		else {
			source.getSource().put(MANAGEMENT_PORT_PROPERTY, "");
		}
	}

	/**
     * Checks if the test server port is fixed.
     * 
     * @param source the map property source
     * @param environment the configurable environment
     * @return {@code true} if the test server port is fixed, {@code false} otherwise
     */
    private boolean isTestServerPortFixed(MapPropertySource source, ConfigurableEnvironment environment) {
		return !Integer.valueOf(0).equals(getPropertyAsInteger(source, SERVER_PORT_PROPERTY, environment));
	}

	/**
     * Checks if the test management port is configured in the given property source.
     * 
     * @param source the property source to check
     * @return {@code true} if the test management port is configured, {@code false} otherwise
     */
    private boolean isTestManagementPortConfigured(PropertySource<?> source) {
		return source.getProperty(MANAGEMENT_PORT_PROPERTY) != null;
	}

	/**
     * Retrieves the value of the specified property as an Integer from the given ConfigurableEnvironment.
     * 
     * @param environment the ConfigurableEnvironment from which to retrieve the property value
     * @param property the name of the property to retrieve
     * @param defaultValue the default value to return if the property is not found or cannot be converted to an Integer
     * @return the value of the property as an Integer, or the defaultValue if the property is not found or cannot be converted
     */
    private Integer getPropertyAsInteger(ConfigurableEnvironment environment, String property, Integer defaultValue) {
		return environment.getPropertySources()
			.stream()
			.filter((source) -> !source.getName()
				.equals(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME))
			.map((source) -> getPropertyAsInteger(source, property, environment))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(defaultValue);
	}

	/**
     * Retrieves the value of a property as an Integer from the given PropertySource.
     * If the property is not found or cannot be converted to an Integer, null is returned.
     * 
     * @param source the PropertySource to retrieve the property from
     * @param property the name of the property to retrieve
     * @param environment the ConfigurableEnvironment containing the ConversionService
     * @return the value of the property as an Integer, or null if not found or cannot be converted
     * @throws ConversionFailedException if the value cannot be converted to an Integer
     */
    private Integer getPropertyAsInteger(PropertySource<?> source, String property,
			ConfigurableEnvironment environment) {
		Object value = source.getProperty(property);
		if (value == null) {
			return null;
		}
		if (ClassUtils.isAssignableValue(Integer.class, value)) {
			return (Integer) value;
		}
		try {
			return environment.getConversionService().convert(value, Integer.class);
		}
		catch (ConversionFailedException ex) {
			if (value instanceof String string) {
				return getResolvedValueIfPossible(environment, string);
			}
			throw ex;
		}
	}

	/**
     * Resolves and converts the given value to an Integer if possible.
     * 
     * @param environment the ConfigurableEnvironment to resolve placeholders from
     * @param value the value to be resolved and converted
     * @return the resolved and converted Integer value, or null if conversion is not possible
     */
    private Integer getResolvedValueIfPossible(ConfigurableEnvironment environment, String value) {
		String resolvedValue = environment.resolveRequiredPlaceholders(value);
		return environment.getConversionService().convert(resolvedValue, Integer.class);
	}

}
