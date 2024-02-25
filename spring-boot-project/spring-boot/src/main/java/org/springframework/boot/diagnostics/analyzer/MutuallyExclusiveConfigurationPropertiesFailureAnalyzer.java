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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by a
 * {@link MutuallyExclusiveConfigurationPropertiesException}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class MutuallyExclusiveConfigurationPropertiesFailureAnalyzer
		extends AbstractFailureAnalyzer<MutuallyExclusiveConfigurationPropertiesException> {

	private final ConfigurableEnvironment environment;

	/**
	 * Constructs a new MutuallyExclusiveConfigurationPropertiesFailureAnalyzer with the
	 * specified environment.
	 * @param environment the environment to be used by the failure analyzer
	 */
	MutuallyExclusiveConfigurationPropertiesFailureAnalyzer(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * Analyzes the failure caused by a MutuallyExclusiveConfigurationPropertiesException.
	 * @param rootFailure The root cause of the failure.
	 * @param cause The MutuallyExclusiveConfigurationPropertiesException that caused the
	 * failure.
	 * @return A FailureAnalysis object containing the analysis of the failure.
	 */
	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MutuallyExclusiveConfigurationPropertiesException cause) {
		List<Descriptor> descriptors = new ArrayList<>();
		for (String name : cause.getConfiguredNames()) {
			List<Descriptor> descriptorsForName = getDescriptors(name);
			if (descriptorsForName.isEmpty()) {
				return null;
			}
			descriptors.addAll(descriptorsForName);
		}
		StringBuilder description = new StringBuilder();
		appendDetails(description, cause, descriptors);
		return new FailureAnalysis(description.toString(),
				"Update your configuration so that only one of the mutually exclusive properties is configured.",
				cause);
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
	 * Appends details about mutually exclusive configuration properties to the given
	 * message.
	 * @param message the StringBuilder to append the details to
	 * @param cause the MutuallyExclusiveConfigurationPropertiesException that caused the
	 * failure
	 * @param descriptors the list of Descriptors representing the configured properties
	 */
	private void appendDetails(StringBuilder message, MutuallyExclusiveConfigurationPropertiesException cause,
			List<Descriptor> descriptors) {
		descriptors.sort(Comparator.comparing((descriptor) -> descriptor.propertyName));
		message.append(String.format("The following configuration properties are mutually exclusive:%n%n"));
		sortedStrings(cause.getMutuallyExclusiveNames())
			.forEach((name) -> message.append(String.format("\t%s%n", name)));
		message.append(String.format("%n"));
		message.append(
				String.format("However, more than one of those properties has been configured at the same time:%n%n"));
		Set<String> configuredDescriptions = sortedStrings(descriptors,
				(descriptor) -> String.format("\t%s%s%n", descriptor.propertyName,
						(descriptor.origin != null) ? " (originating from '" + descriptor.origin + "')" : ""));
		configuredDescriptions.forEach(message::append);
	}

	/**
	 * Returns a sorted set of strings based on the given input collection.
	 * @param input the input collection of strings
	 * @return a sorted set of strings
	 */
	private Set<String> sortedStrings(Collection<String> input) {
		return sortedStrings(input, Function.identity());
	}

	/**
	 * Sorts a collection of objects and converts them to strings using a provided
	 * converter function.
	 * @param <S> the type of objects in the collection
	 * @param input the collection of objects to be sorted and converted
	 * @param converter the function used to convert objects to strings
	 * @return a sorted set of strings converted from the input collection
	 */
	private <S> Set<String> sortedStrings(Collection<S> input, Function<S, String> converter) {
		TreeSet<String> results = new TreeSet<>();
		for (S item : input) {
			results.add(converter.apply(item));
		}
		return results;
	}

	/**
	 * Descriptor class.
	 */
	private static final class Descriptor {

		private final String propertyName;

		private final Origin origin;

		/**
		 * Constructs a new Descriptor with the specified property name and origin.
		 * @param propertyName the name of the property
		 * @param origin the origin of the property
		 */
		private Descriptor(String propertyName, Origin origin) {
			this.propertyName = propertyName;
			this.origin = origin;
		}

		/**
		 * Retrieves the descriptor for the specified property from the given property
		 * source.
		 * @param source the property source to retrieve the descriptor from
		 * @param propertyName the name of the property
		 * @return the descriptor for the property
		 */
		static Descriptor get(PropertySource<?> source, String propertyName) {
			Origin origin = OriginLookup.getOrigin(source, propertyName);
			return new Descriptor(propertyName, origin);
		}

	}

}
