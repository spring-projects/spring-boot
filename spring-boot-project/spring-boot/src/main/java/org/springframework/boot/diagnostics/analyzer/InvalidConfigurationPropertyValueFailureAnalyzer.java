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

package org.springframework.boot.diagnostics.analyzer;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by an
 * {@link InvalidConfigurationPropertyValueException}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class InvalidConfigurationPropertyValueFailureAnalyzer
		extends AbstractFailureAnalyzer<InvalidConfigurationPropertyValueException> {

	private final ConfigurableEnvironment environment;

	/**
	 * Constructs a new InvalidConfigurationPropertyValueFailureAnalyzer with the
	 * specified environment.
	 * @param environment the environment to be used by the failure analyzer
	 */
	InvalidConfigurationPropertyValueFailureAnalyzer(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * Analyzes the failure caused by an invalid configuration property value.
	 * @param rootFailure the root failure that caused the exception
	 * @param cause the InvalidConfigurationPropertyValueException that occurred
	 * @return a FailureAnalysis object containing the analysis of the failure, or null if
	 * no descriptors are found
	 */
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

	/**
	 * Retrieves a list of descriptors for a given property name.
	 * @param propertyName the name of the property
	 * @return a list of descriptors for the property
	 */
	private List<Descriptor> getDescriptors(String propertyName) {
		return getPropertySources().filter((source) -> source.containsProperty(propertyName))
			.map((source) -> Descriptor.get(source, propertyName))
			.toList();
	}

	/**
	 * Returns a stream of property sources.
	 * @return a stream of property sources
	 */
	private Stream<PropertySource<?>> getPropertySources() {
		if (this.environment == null) {
			return Stream.empty();
		}
		return this.environment.getPropertySources()
			.stream()
			.filter((source) -> !ConfigurationPropertySources.isAttachedConfigurationPropertySource(source));
	}

	/**
	 * Appends details to the given StringBuilder message for an
	 * InvalidConfigurationPropertyValueException.
	 * @param message the StringBuilder message to append details to
	 * @param cause the InvalidConfigurationPropertyValueException that occurred
	 * @param descriptors the list of Descriptors associated with the exception
	 */
	private void appendDetails(StringBuilder message, InvalidConfigurationPropertyValueException cause,
			List<Descriptor> descriptors) {
		Descriptor mainDescriptor = descriptors.get(0);
		message.append("Invalid value '").append(mainDescriptor.getValue()).append("' for configuration property '");
		message.append(cause.getName()).append("'");
		mainDescriptor.appendOrigin(message);
		message.append(".");
	}

	/**
	 * Appends the reason for the validation failure to the given message.
	 * @param message the StringBuilder to append the reason to
	 * @param cause the InvalidConfigurationPropertyValueException that contains the
	 * reason for the validation failure
	 */
	private void appendReason(StringBuilder message, InvalidConfigurationPropertyValueException cause) {
		if (StringUtils.hasText(cause.getReason())) {
			message.append(String.format(" Validation failed for the following reason:%n%n"));
			message.append(cause.getReason());
		}
		else {
			message.append(" No reason was provided.");
		}
	}

	/**
	 * Appends additional properties to the given message.
	 * @param message the StringBuilder object to append the properties to
	 * @param descriptors the list of Descriptor objects representing the additional
	 * properties
	 */
	private void appendAdditionalProperties(StringBuilder message, List<Descriptor> descriptors) {
		List<Descriptor> others = descriptors.subList(1, descriptors.size());
		if (!others.isEmpty()) {
			message
				.append(String.format("%n%nAdditionally, this property is also set in the following property %s:%n%n",
						(others.size() > 1) ? "sources" : "source"));
			for (Descriptor other : others) {
				message.append("\t- In '").append(other.getPropertySource()).append("'");
				message.append(" with the value '").append(other.getValue()).append("'");
				other.appendOrigin(message);
				message.append(String.format(".%n"));
			}
		}
	}

	/**
	 * Returns the action to be taken for an InvalidConfigurationPropertyValueException.
	 * @param cause the InvalidConfigurationPropertyValueException that occurred
	 * @return the action to be taken
	 */
	private String getAction(InvalidConfigurationPropertyValueException cause) {
		StringBuilder action = new StringBuilder();
		action.append("Review the value of the property");
		if (cause.getReason() != null) {
			action.append(" with the provided reason");
		}
		action.append(".");
		return action.toString();
	}

	/**
	 * Descriptor class.
	 */
	private static final class Descriptor {

		private final String propertySource;

		private final Object value;

		private final Origin origin;

		/**
		 * Constructs a new Descriptor with the specified property source, value, and
		 * origin.
		 * @param propertySource the source of the property
		 * @param value the value of the property
		 * @param origin the origin of the property
		 */
		private Descriptor(String propertySource, Object value, Origin origin) {
			this.propertySource = propertySource;
			this.value = value;
			this.origin = origin;
		}

		/**
		 * Returns the property source of the Descriptor.
		 * @return the property source of the Descriptor
		 */
		String getPropertySource() {
			return this.propertySource;
		}

		/**
		 * Returns the value of the Descriptor object.
		 * @return the value of the Descriptor object
		 */
		Object getValue() {
			return this.value;
		}

		/**
		 * Appends the origin of the message to the provided StringBuilder. If the origin
		 * is not null, it appends the origin in the format: " (originating from
		 * '{origin}')".
		 * @param message the StringBuilder to which the origin will be appended
		 */
		void appendOrigin(StringBuilder message) {
			if (this.origin != null) {
				message.append(" (originating from '").append(this.origin).append("')");
			}
		}

		/**
		 * Retrieves the descriptor for a given property from a property source.
		 * @param source the property source from which to retrieve the property
		 * @param propertyName the name of the property to retrieve
		 * @return the descriptor containing information about the property
		 */
		static Descriptor get(PropertySource<?> source, String propertyName) {
			Object value = source.getProperty(propertyName);
			Origin origin = OriginLookup.getOrigin(source, propertyName);
			return new Descriptor(source.getName(), value, origin);
		}

	}

}
