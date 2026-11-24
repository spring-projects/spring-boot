/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.migrator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * Provides a properties migration report.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Steve Rutherford
 */
class PropertiesMigrationReport {

	private final Map<String, LegacyProperties> content = new LinkedHashMap<>();

	/**
	 * Return a report for properties that were automatically renamed or are deprecated
	 * but still supported. If no such properties were found, return {@code null}.
	 * @return a report with the configuration keys that should be migrated
	 */
	@Nullable String getWarningReport() {
		Map<String, List<PropertyMigration>> content = getContent(LegacyProperties::getRenamed);
		Map<String, List<PropertyMigration>> deprecated = getContent(LegacyProperties::getDeprecated);
		if (content.isEmpty() && deprecated.isEmpty()) {
			return null;
		}
		StringBuilder report = new StringBuilder();
		if (!content.isEmpty()) {
			report.append(String
				.format("%nThe use of configuration keys that have been renamed was found in the environment:%n%n"));
			append(report, content);
			report.append(String.format("%n"));
			report.append("Each configuration key has been temporarily mapped to its "
					+ "replacement for your convenience. To silence this warning, please "
					+ "update your configuration to use the new keys.");
			report.append(String.format("%n"));
		}
		if (!deprecated.isEmpty()) {
			report.append(String
				.format("%nThe use of configuration keys that are deprecated was found in the environment:%n%n"));
			append(report, deprecated);
			report.append(String.format("%n"));
			report.append("These configuration keys are still supported but could not be migrated automatically. "
					+ "Please refer to the release notes or reference guide for potential alternatives.");
			report.append(String.format("%n"));
		}
		return report.toString();
	}

	/**
	 * Return a report for all the properties that are no longer supported. If no such
	 * properties were found, return {@code null}.
	 * @return a report with the configurations keys that are no longer supported
	 */
	@Nullable String getErrorReport() {
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

	private Map<String, List<PropertyMigration>> getContent(
			Function<LegacyProperties, List<PropertyMigration>> extractor) {
		return this.content.entrySet()
			.stream()
			.filter((entry) -> !extractor.apply(entry.getValue()).isEmpty())
			.collect(
					Collectors.toMap(Map.Entry::getKey, (entry) -> new ArrayList<>(extractor.apply(entry.getValue()))));
	}

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

	private static class LegacyProperties {

		private final List<PropertyMigration> properties;

		LegacyProperties(List<PropertyMigration> properties) {
			this.properties = new ArrayList<>(properties);
		}

		List<PropertyMigration> getRenamed() {
			return this.properties.stream().filter(PropertyMigration::isCompatibleType).toList();
		}

		List<PropertyMigration> getUnsupported() {
			return getUnmapped(Level.ERROR);
		}

		List<PropertyMigration> getDeprecated() {
			return getUnmapped(Level.WARNING);
		}

		private List<PropertyMigration> getUnmapped(Level level) {
			return this.properties.stream()
				.filter((property) -> !property.isCompatibleType())
				.filter((property) -> property.getMetadata().getDeprecation().getLevel() == level)
				.toList();
		}

	}

}
