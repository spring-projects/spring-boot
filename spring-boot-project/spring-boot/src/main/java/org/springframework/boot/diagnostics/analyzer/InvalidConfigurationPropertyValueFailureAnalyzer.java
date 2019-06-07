/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by an
 * {@link InvalidConfigurationPropertyValueException}.
 *
 * @author Stephane Nicoll
 */
class InvalidConfigurationPropertyValueFailureAnalyzer
		extends AbstractFailureAnalyzer<InvalidConfigurationPropertyValueException> implements EnvironmentAware {

	private ConfigurableEnvironment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationPropertyValueException cause) {
		List<Descriptor> descriptors = getDescriptors(cause.getName());
		if (descriptors.isEmpty()) {
			return null;
		}
		StringBuilder description = new StringBuilder();
		appendDetails(description, cause, descriptors);
		appendReason(description, cause);
		appendAdditionalProperties(description, descriptors);
		return new FailureAnalysis(description.toString(), getAction(cause), cause);
	}

	private List<Descriptor> getDescriptors(String propertyName) {
		return getPropertySources().filter((source) -> source.containsProperty(propertyName))
				.map((source) -> Descriptor.get(source, propertyName)).collect(Collectors.toList());
	}

	private Stream<PropertySource<?>> getPropertySources() {
		Iterable<PropertySource<?>> sources = (this.environment != null) ? this.environment.getPropertySources()
				: Collections.emptyList();
		return StreamSupport.stream(sources.spliterator(), false)
				.filter((source) -> !ConfigurationPropertySources.isAttachedConfigurationPropertySource(source));
	}

	private void appendDetails(StringBuilder message, InvalidConfigurationPropertyValueException cause,
			List<Descriptor> descriptors) {
		Descriptor mainDescriptor = descriptors.get(0);
		message.append("Invalid value '" + mainDescriptor.getValue() + "' for configuration property '"
				+ cause.getName() + "'");
		mainDescriptor.appendOrigin(message);
		message.append(".");
	}

	private void appendReason(StringBuilder message, InvalidConfigurationPropertyValueException cause) {
		if (StringUtils.hasText(cause.getReason())) {
			message.append(String.format(" Validation failed for the following " + "reason:%n%n"));
			message.append(cause.getReason());
		}
		else {
			message.append(" No reason was provided.");
		}
	}

	private void appendAdditionalProperties(StringBuilder message, List<Descriptor> descriptors) {
		List<Descriptor> others = descriptors.subList(1, descriptors.size());
		if (!others.isEmpty()) {
			message.append(
					String.format("%n%nAdditionally, this property is also set in the following " + "property %s:%n%n",
							(others.size() > 1) ? "sources" : "source"));
			for (Descriptor other : others) {
				message.append("\t- In '" + other.getPropertySource() + "'");
				message.append(" with the value '" + other.getValue() + "'");
				other.appendOrigin(message);
				message.append(String.format(".%n"));
			}
		}
	}

	private String getAction(InvalidConfigurationPropertyValueException cause) {
		StringBuilder action = new StringBuilder();
		action.append("Review the value of the property");
		if (cause.getReason() != null) {
			action.append(" with the provided reason");
		}
		action.append(".");
		return action.toString();
	}

	private static final class Descriptor {

		private final String propertySource;

		private final Object value;

		private final Origin origin;

		private Descriptor(String propertySource, Object value, Origin origin) {
			this.propertySource = propertySource;
			this.value = value;
			this.origin = origin;
		}

		public String getPropertySource() {
			return this.propertySource;
		}

		public Object getValue() {
			return this.value;
		}

		public void appendOrigin(StringBuilder message) {
			if (this.origin != null) {
				message.append(" (originating from '" + this.origin + "')");
			}
		}

		static Descriptor get(PropertySource<?> source, String propertyName) {
			Object value = source.getProperty(propertyName);
			Origin origin = OriginLookup.getOrigin(source, propertyName);
			return new Descriptor(source.getName(), value, origin);
		}

	}

}
