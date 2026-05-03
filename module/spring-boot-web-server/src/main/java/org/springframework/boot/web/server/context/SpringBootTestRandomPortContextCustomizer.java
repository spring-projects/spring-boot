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

package org.springframework.boot.web.server.context;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.lang.Contract;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link SmartApplicationListener} for tests that starts the management context on a
 * random port if the main server's port is 0 and the management context is expected on a
 * different port.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class SpringBootTestRandomPortContextCustomizer implements ContextCustomizer {

	private static final String TEST_SOURCE_NAME = TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME;

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		postProcessEnvironment(context.getEnvironment());
	}

	void postProcessEnvironment(ConfigurableEnvironment environment) {
		MapPropertySource testSource = (MapPropertySource) environment.getPropertySources().get(TEST_SOURCE_NAME);
		if (testSource != null) {
			MutablePropertySources nonTestSources = new MutablePropertySources(environment.getPropertySources());
			nonTestSources.remove(TEST_SOURCE_NAME);
			Ports ports = new Ports(environment, environment.getConversionService());
			String value = getManagementServerPortPropertyValue(testSource, nonTestSources, ports);
			if (value != null) {
				testSource.getSource().put(Port.MANAGEMENT.property(), value);
			}
		}
	}

	private @Nullable String getManagementServerPortPropertyValue(MapPropertySource testSource,
			MutablePropertySources nonTestSources, Ports ports) {
		if (ports.isFixed(testSource, Port.SERVER) || ports.isConfigured(testSource, Port.MANAGEMENT)) {
			return null;
		}
		Integer managementPort = ports.get(nonTestSources, Port.MANAGEMENT, null);
		if (managementPort == null || managementPort.equals(-1) || managementPort.equals(0)) {
			return null;
		}
		Integer serverPort = ports.get(nonTestSources, Port.SERVER, 8080);
		return (!managementPort.equals(serverPort)) ? "0" : "";
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (obj.getClass() == getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	private enum Port {

		SERVER("server.port"), MANAGEMENT("management.server.port");

		private final String property;

		Port(String property) {
			this.property = property;
		}

		String property() {
			return this.property;
		}

	}

	private record Ports(PropertyResolver resolver, ConversionService conversionService) {

		private static final Integer ZERO = Integer.valueOf(0);

		boolean isFixed(MapPropertySource source, Port port) {
			return !ZERO.equals(get(source, port));
		}

		boolean isConfigured(PropertySource<?> source, Port port) {
			return source.getProperty(port.property()) != null;
		}

		@Contract("_, _, !null -> !null")
		@Nullable Integer get(PropertySources sources, Port port, @Nullable Integer defaultValue) {
			return sources.stream()
				.map((source) -> get(source, port))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(defaultValue);
		}

		@Nullable Integer get(PropertySource<?> source, Port port) {
			Object value = source.getProperty(port.property());
			if (value == null || ClassUtils.isAssignableValue(Integer.class, value)) {
				return (Integer) value;
			}
			try {
				return asInteger(value);
			}
			catch (ConversionFailedException ex) {
				if (value instanceof String string) {
					return asInteger(resolver().resolveRequiredPlaceholders(string));
				}
				throw ex;
			}
		}

		private @Nullable Integer asInteger(@Nullable Object value) {
			return conversionService().convert(value, Integer.class);
		}

	}

}
