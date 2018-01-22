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

package org.springframework.boot.configurationalayzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.util.StringUtils;

/**
 * Describes the outcome of the environment analysis.
 *
 * @author Stephane Nicoll
 */
class LegacyPropertiesAnalysis {

	private final Map<String, PropertySourceAnalysis> content = new LinkedHashMap<>();

	/**
	 * Create a report for all the legacy properties that were automatically renamed. If
	 * no such legacy properties were found, return {@code null}.
	 * @return a report with the configurations keys that should be renamed
	 */
	public String createWarningReport() {
		Map<String, List<LegacyProperty>> content = this.content.entrySet().stream()
				.filter(e -> !e.getValue().handledProperties.isEmpty())
				.collect(Collectors.toMap(Map.Entry::getKey,
						e -> new ArrayList<>(e.getValue().handledProperties)));
		if (content.isEmpty()) {
			return null;
		}
		StringBuilder report = new StringBuilder();
		report.append(String.format("%nThe use of configuration keys that have been "
				+ "renamed was found in the environment:%n%n"));
		appendProperties(report, content, metadata ->
				"Replacement: " + metadata.getDeprecation().getReplacement());
		report.append(String.format("%n"));
		report.append("Each configuration key has been temporarily mapped to its "
				+ "replacement for your convenience. To silence this warning, please "
				+ "update your configuration to use the new keys.");
		report.append(String.format("%n"));
		return report.toString();
	}

	/**
	 * Create a report for all the legacy properties that are no longer supported. If
	 * no such legacy properties were found, return {@code null}.
	 * @return a report with the configurations keys that are no longer supported
	 */
	public String createErrorReport() {
		Map<String, List<LegacyProperty>> content = this.content.entrySet().stream()
				.filter(e -> !e.getValue().notHandledProperties.isEmpty())
				.collect(Collectors.toMap(Map.Entry::getKey,
						e -> new ArrayList<>(e.getValue().notHandledProperties)));
		if (content.isEmpty()) {
			return null;
		}
		StringBuilder report = new StringBuilder();
		report.append(String.format("%nThe use of configuration keys that are no longer "
				+ "supported was found in the environment:%n%n"));
		appendProperties(report, content, metadata ->
				"Reason: " + (StringUtils.hasText(metadata.getDeprecation().getReason())
						? metadata.getDeprecation().getReason() : "none"));
		report.append(String.format("%n"));
		report.append("Please refer to the migration guide or reference guide for "
				+ "potential alternatives.");
		report.append(String.format("%n"));
		return report.toString();
	}

	private void appendProperties(StringBuilder report,
			Map<String, List<LegacyProperty>> content,
			Function<ConfigurationMetadataProperty, String> deprecationMessage) {
		content.forEach((name, properties) -> {
			report.append(String.format("Property source '%s':%n", name));
			properties.sort(LegacyProperty.COMPARATOR);
			properties.forEach((property) -> {
				ConfigurationMetadataProperty metadata = property.getMetadata();
				report.append(String.format("\tKey: %s%n", metadata.getId()));
				if (property.getLineNumber() != null) {
					report.append(String.format("\t\tLine: %d%n",
							property.getLineNumber()));
				}
				report.append(String.format("\t\t%s%n",
						deprecationMessage.apply(metadata)));
			});
			report.append(String.format("%n"));
		});
	}

	/**
	 * Register a new property source.
	 * @param name the name of the property source
	 * @param handledProperties the properties that were renamed
	 * @param notHandledProperties the properties that are no longer supported
	 */
	void register(String name, List<LegacyProperty> handledProperties,
			List<LegacyProperty> notHandledProperties) {
		List<LegacyProperty> handled = (handledProperties != null
				? new ArrayList<>(handledProperties) : Collections.emptyList());
		List<LegacyProperty> notHandled = (notHandledProperties != null
				? new ArrayList<>(notHandledProperties) : Collections.emptyList());
		this.content.put(name, new PropertySourceAnalysis(handled, notHandled));
	}


	private static class PropertySourceAnalysis {

		private final List<LegacyProperty> handledProperties;

		private final List<LegacyProperty> notHandledProperties;

		PropertySourceAnalysis(List<LegacyProperty> handledProperties,
				List<LegacyProperty> notHandledProperties) {
			this.handledProperties = handledProperties;
			this.notHandledProperties = notHandledProperties;
		}

	}

}
