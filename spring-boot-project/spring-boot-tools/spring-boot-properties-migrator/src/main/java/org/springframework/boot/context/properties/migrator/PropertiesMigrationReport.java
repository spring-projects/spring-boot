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

package org.springframework.boot.context.properties.migrator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides a properties migration report.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class PropertiesMigrationReport {

	private final Map<String, LegacyProperties> content = new LinkedHashMap<>();

	/**
	 * Return a report for all the properties that were automatically renamed. If no such
	 * properties were found, return {@code null}.
	 * @return a report with the configurations keys that should be renamed
	 */
	String getWarningReport() {
		Map<String, List<PropertyMigration>> content = getContent(LegacyProperties::getRenamed);
		if (content.isEmpty()) {
			return null;
		}
		StringBuilder report = new StringBuilder();
		report.append(String
			.format("%nThe use of configuration keys that have been renamed was found in the environment:%n%n"));
		append(report, content);
		report.append(String.format("%n"));
		report.append("Each configuration key has been temporarily mapped to its "
				+ "replacement for your convenience. To silence this warning, please "
				+ "update your configuration to use the new keys.");
		report.append(String.format("%n"));
		return report.toString();
	}

	/**
	 * Return a report for all the properties that are no longer supported. If no such
	 * properties were found, return {@code null}.
	 * @return a report with the configurations keys that are no longer supported
	 */
	String getErrorReport() {
		Map<String, List<PropertyMigration>> content = getContent(LegacyProperties::getUnsupported);
		if (content.isEmpty()) {
			return null;
		}
		StringBuilder report = new StringBuilder();
		report.append(String
			.format("%nThe use of configuration keys that are no longer supported was found in the environment:%n%n"));
		append(report, content);
		report.append(String.format("%n"));
		report.append("Please refer to the release notes or reference guide for potential alternatives.");
		report.append(String.format("%n"));
		return report.toString();
	}

	/**
	 * Retrieves the content of the PropertiesMigrationReport by applying the given
	 * extractor function to each entry in the content map.
	 * @param extractor the function used to extract the property migrations from a
	 * LegacyProperties object
	 * @return a map containing the extracted property migrations for each entry in the
	 * content map
	 */
	private Map<String, List<PropertyMigration>> getContent(
			Function<LegacyProperties, List<PropertyMigration>> extractor) {
		return this.content.entrySet()
			.stream()
			.filter((entry) -> !extractor.apply(entry.getValue()).isEmpty())
			.collect(
					Collectors.toMap(Map.Entry::getKey, (entry) -> new ArrayList<>(extractor.apply(entry.getValue()))));
	}

	/**
	 * Appends the content of a map of property migrations to a StringBuilder report.
	 * @param report the StringBuilder report to append the content to
	 * @param content the map containing the property migrations, where the key is the
	 * property source name and the value is a list of PropertyMigration objects
	 */
	private void append(StringBuilder report, Map<String, List<PropertyMigration>> content) {
		content.forEach((name, properties) -> {
			report.append(String.format("Property source '%s':%n", name));
			properties.sort(PropertyMigration.COMPARATOR);
			properties.forEach((property) -> {
				report.append(String.format("\tKey: %s%n", property.getProperty().getName()));
				if (property.getLineNumber() != null) {
					report.append(String.format("\t\tLine: %d%n", property.getLineNumber()));
				}
				report.append(String.format("\t\t%s%n", property.determineReason()));
			});
			report.append(String.format("%n"));
		});
	}

	/**
	 * Register a new property source.
	 * @param name the name of the property source
	 * @param properties the {@link PropertyMigration} instances
	 */
	void add(String name, List<PropertyMigration> properties) {
		this.content.put(name, new LegacyProperties(properties));
	}

	/**
	 * LegacyProperties class.
	 */
	private static class LegacyProperties {

		private final List<PropertyMigration> properties;

		/**
		 * Constructs a new LegacyProperties object with the given list of
		 * PropertyMigration objects.
		 * @param properties the list of PropertyMigration objects to be used for
		 * initializing the LegacyProperties object
		 */
		LegacyProperties(List<PropertyMigration> properties) {
			this.properties = new ArrayList<>(properties);
		}

		/**
		 * Returns a list of PropertyMigration objects representing properties that have
		 * been renamed.
		 * @return a list of PropertyMigration objects representing properties that have
		 * been renamed
		 */
		List<PropertyMigration> getRenamed() {
			return this.properties.stream().filter(PropertyMigration::isCompatibleType).toList();
		}

		/**
		 * Returns a list of PropertyMigration objects that represent the properties in
		 * the LegacyProperties class that have unsupported types.
		 * @return a List of PropertyMigration objects representing the unsupported
		 * properties
		 */
		List<PropertyMigration> getUnsupported() {
			return this.properties.stream().filter((property) -> !property.isCompatibleType()).toList();
		}

	}

}
