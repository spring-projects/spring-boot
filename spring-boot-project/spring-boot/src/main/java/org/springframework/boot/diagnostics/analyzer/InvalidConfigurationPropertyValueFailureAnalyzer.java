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

package org.springframework.boot.diagnostics.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by a
 * {@link InvalidConfigurationPropertyValueException}.

 * @author Stephane Nicoll
 */
class InvalidConfigurationPropertyValueFailureAnalyzer
		extends AbstractFailureAnalyzer<InvalidConfigurationPropertyValueException>
		implements EnvironmentAware {

	private ConfigurableEnvironment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			InvalidConfigurationPropertyValueException cause) {
		List<PropertyValueDescriptor> descriptors = getPropertySourceDescriptors(cause.getName());
		if (descriptors.isEmpty()) {
			return null;
		}
		PropertyValueDescriptor main = descriptors.get(0);
		StringBuilder message = new StringBuilder();
		message.append(String.format("The value '%s'", main.value));
		if (main.origin != null) {
			message.append(String.format(" from origin '%s'", main.origin));
		}
		message.append(String.format(" of configuration property '%s' was invalid. ",
				cause.getName()));
		if (StringUtils.hasText(cause.getReason())) {
			message.append(String.format(
					"Validation failed for the following reason:%n%n"));
			message.append(cause.getReason());
		}
		else {
			message.append("No reason was provided.");
		}
		List<PropertyValueDescriptor> others = descriptors.subList(1, descriptors.size());
		if (!others.isEmpty()) {
			message.append(String.format(
					"%n%nAdditionally, this property is also set in the following "
							+ "property %s:%n%n",
					others.size() > 1 ? "sources" : "source"));
			for (PropertyValueDescriptor other : others) {
				message.append(String.format("\t- %s: %s%n", other.propertySource,
						other.value));
			}
		}
		StringBuilder action = new StringBuilder();
		action.append("Review the value of the property");
		if (cause.getReason() != null) {
			action.append(" with the provided reason");
		}
		action.append(".");
		return new FailureAnalysis(message.toString(), action.toString(), cause);
	}

	private List<PropertyValueDescriptor> getPropertySourceDescriptors(
			String propertyName) {
		List<PropertyValueDescriptor> propertySources = new ArrayList<>();
		getPropertySourcesAsMap()
				.forEach((sourceName, source) -> {
					if (source.containsProperty(propertyName)) {
						propertySources.add(describeValueOf(propertyName, source));
					}
				});
		return propertySources;
	}

	private Map<String, PropertySource<?>> getPropertySourcesAsMap() {
		Map<String, PropertySource<?>> map = new LinkedHashMap<>();
		if (this.environment != null) {
			for (PropertySource<?> source : this.environment.getPropertySources()) {
				if (!ConfigurationPropertySources
						.isAttachedConfigurationPropertySource(source)) {
					map.put(source.getName(), source);
				}
			}
		}
		return map;
	}

	private PropertyValueDescriptor describeValueOf(String name,
			PropertySource<?> source) {
		Object value = source.getProperty(name);
		String origin = (source instanceof OriginLookup)
				? ((OriginLookup<Object>) source).getOrigin(name).toString() : null;
		return new PropertyValueDescriptor(source.getName(), value, origin);
	}

	private static class PropertyValueDescriptor {

		private final String propertySource;

		private final Object value;

		private final String origin;

		PropertyValueDescriptor(String propertySource,
				Object value, String origin) {
			this.propertySource = propertySource;
			this.value = value;
			this.origin = origin;
		}

	}

}
