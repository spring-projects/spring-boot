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

package org.springframework.boot.context.properties.migrator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.util.StringUtils;

/**
 * Provides a properties migration report.
 *
 * @author Stephane Nicoll
 */
class PropertiesMigrationReport {

	private final Map<String, LegacyProperties> content = new LinkedHashMap<>();

	/**
	 * Return a report for all the properties that were automatically renamed. If no such
	 * properties were found, return {@code null}.
	 * @return a report with the configurations keys that should be renamed
	 */
	public String getWarningReport() {
		Map<String, List<PropertyMigration>> content = getContent(
				LegacyProperties::getRenamed);
		if (content.isEmpty()) {
			return null;
		}
		StringBuilder report = new StringBuilder();
		report.append(String.format("%nThe use of configuration keys that have been "
				+ "renamed was found in the environment:%n%n"));
		append(report, content, (metadata) -> "Replacement: "
				+ metadata.getDeprecation().getReplacement());
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
	public String getErrorReport() {
		Map<String, List<PropertyMigration>> content = getContent(
				LegacyProperties::getUnsupported);
		if (content.isEmpty()) {
			return null;
		}
		StringBuilder report = new StringBuilder();
		report.append(String.format("%nThe use of configuration keys that are no longer "
				+ "supported was found in the environment:%n%n"));
		append(report, content, this::determineReason);
		report.append(String.format("%n"));
		report.append("Please refer to the migration guide or reference guide for "
				+ "potential alternatives.");
		report.append(String.format("%n"));
		return report.toString();
	}

	private String determineReason(ConfigurationMetadataProperty metadata) {
		Deprecation deprecation = metadata.getDeprecation();
		if (StringUtils.hasText(deprecation.getShortReason())) {
			return "Reason: " + deprecation.getShortReason();
		}
		if (StringUtils.hasText(deprecation.getReplacement())) {
			return String.format(
					"Reason: Replacement key '%s' uses an incompatible " + "target type",
					deprecation.getReplacement());
		}
		return "Reason: none";
	}

	private Map<String, List<PropertyMigration>> getContent(
			Function<LegacyProperties, List<PropertyMigration>> extractor) {
		return this.content.entrySet().stream()
				.filter((entry) -> !extractor.apply(entry.getValue()).isEmpty())
				.collect(Collectors.toMap(Map.Entry::getKey,
						(entry) -> new ArrayList<>(extractor.apply(entry.getValue()))));
	}

	private void append(StringBuilder report,
			Map<String, List<PropertyMigration>> content,
			Function<ConfigurationMetadataProperty, String> deprecationMessage) {
		content.forEach((name, properties) -> {
			report.append(String.format("Property source '%s':%n", name));
			properties.sort(PropertyMigration.COMPARATOR);
			properties.forEach((property) -> {
				ConfigurationMetadataProperty metadata = property.getMetadata();
				report.append(String.format("\tKey: %s%n", metadata.getId()));
				if (property.getLineNumber() != null) {
					report.append(
							String.format("\t\tLine: %d%n", property.getLineNumber()));
				}
				report.append(
						String.format("\t\t%s%n", deprecationMessage.apply(metadata)));
			});
			report.append(String.format("%n"));
		});
	}

	/**
	 * Register a new property source.
	 * @param name the name of the property source
	 * @param renamed the properties that were renamed
	 * @param unsupported the properties that are no longer supported
	 */
	void add(String name, List<PropertyMigration> renamed,
			List<PropertyMigration> unsupported) {
		this.content.put(name, new LegacyProperties(renamed, unsupported));
	}

	private static class LegacyProperties {

		private final List<PropertyMigration> renamed;

		private final List<PropertyMigration> unsupported;

		LegacyProperties(List<PropertyMigration> renamed,
				List<PropertyMigration> unsupported) {
			this.renamed = asNewList(renamed);
			this.unsupported = asNewList(unsupported);
		}

		private <T> List<T> asNewList(List<T> source) {
			return (source != null) ? new ArrayList<>(source) : Collections.emptyList();
		}

		public List<PropertyMigration> getRenamed() {
			return this.renamed;
		}

		public List<PropertyMigration> getUnsupported() {
			return this.unsupported;
		}

	}

}
