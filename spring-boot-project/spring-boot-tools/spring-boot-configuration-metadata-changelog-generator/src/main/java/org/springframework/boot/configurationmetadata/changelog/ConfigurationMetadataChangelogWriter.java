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

package org.springframework.boot.configurationmetadata.changelog;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.configurationmetadata.changelog.ConfigurationMetadataDiff.Difference;
import org.springframework.boot.configurationmetadata.changelog.ConfigurationMetadataDiff.Difference.Type;

/**
 * Writes a configuration metadata changelog from a {@link ConfigurationMetadataDiff}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ConfigurationMetadataChangelogWriter implements AutoCloseable {

	private final PrintWriter out;

	ConfigurationMetadataChangelogWriter(Writer out) {
		this.out = new PrintWriter(out);
	}

	void write(ConfigurationMetadataDiff diff) {
		this.out.append(String.format("Configuration property changes between `%s` and " + "`%s`%n", diff.leftName(),
				diff.rightName()));
		this.out.append(System.lineSeparator());
		this.out.append(String.format("== Deprecated in `%s`%n", diff.rightName()));
		Map<Type, List<Difference>> differencesByType = differencesByType(diff);
		writeDeprecatedProperties(differencesByType.get(Type.DEPRECATED));
		this.out.append(System.lineSeparator());
		this.out.append(String.format("== New in `%s`%n", diff.rightName()));
		writeAddedProperties(differencesByType.get(Type.ADDED));
		this.out.append(System.lineSeparator());
		this.out.append(String.format("== Removed in `%s`%n", diff.rightName()));
		writeRemovedProperties(differencesByType.get(Type.DELETED), differencesByType.get(Type.DEPRECATED));
	}

	private Map<Type, List<Difference>> differencesByType(ConfigurationMetadataDiff diff) {
		Map<Type, List<Difference>> differencesByType = new HashMap<>();
		for (Type type : Type.values()) {
			differencesByType.put(type, new ArrayList<>());
		}
		for (Difference difference : diff.differences()) {
			differencesByType.get(difference.type()).add(difference);
		}
		return differencesByType;
	}

	private void writeDeprecatedProperties(List<Difference> differences) {
		if (differences.isEmpty()) {
			this.out.append(String.format("None.%n"));
		}
		else {
			List<Difference> properties = sortProperties(differences, Difference::right).stream()
				.filter(this::isDeprecatedInRelease)
				.collect(Collectors.toList());
			this.out.append(String.format("|======================%n"));
			this.out.append(String.format("|Key  |Replacement |Reason%n"));
			properties.forEach((diff) -> {
				ConfigurationMetadataProperty property = diff.right();
				writeDeprecatedProperty(property);
			});
			this.out.append(String.format("|======================%n"));
		}
		this.out.append(String.format("%n%n"));
	}

	private boolean isDeprecatedInRelease(Difference difference) {
		return difference.right().getDeprecation() != null
				&& Deprecation.Level.ERROR != difference.right().getDeprecation().getLevel();
	}

	private void writeAddedProperties(List<Difference> differences) {
		if (differences.isEmpty()) {
			this.out.append(String.format("None.%n"));
		}
		else {
			List<Difference> properties = sortProperties(differences, Difference::right);
			this.out.append(String.format("|======================%n"));
			this.out.append(String.format("|Key  |Default value |Description%n"));
			properties.forEach((diff) -> writeRegularProperty(diff.right()));
			this.out.append(String.format("|======================%n"));
		}
		this.out.append(String.format("%n%n"));
	}

	private void writeRemovedProperties(List<Difference> deleted, List<Difference> deprecated) {
		List<Difference> removed = getRemovedProperties(deleted, deprecated);
		if (removed.isEmpty()) {
			this.out.append(String.format("None.%n"));
		}
		else {
			this.out.append(String.format("|======================%n"));
			this.out.append(String.format("|Key  |Replacement |Reason%n"));
			removed.forEach((property) -> writeDeprecatedProperty(
					(property.right() != null) ? property.right() : property.left()));
			this.out.append(String.format("|======================%n"));
		}
	}

	private List<Difference> getRemovedProperties(List<Difference> deleted, List<Difference> deprecated) {
		List<Difference> properties = new ArrayList<>(deleted);
		properties.addAll(deprecated.stream().filter((p) -> !isDeprecatedInRelease(p)).collect(Collectors.toList()));
		return sortProperties(properties,
				(difference) -> (difference.left() != null) ? difference.left() : difference.right());
	}

	private void writeRegularProperty(ConfigurationMetadataProperty property) {
		this.out.append("|`").append(property.getId()).append("` |");
		if (property.getDefaultValue() != null) {
			this.out.append("`").append(defaultValueToString(property.getDefaultValue())).append("`");
		}
		this.out.append(" |");
		if (property.getDescription() != null) {
			this.out.append(property.getShortDescription());
		}
		this.out.append(System.lineSeparator());
	}

	private void writeDeprecatedProperty(ConfigurationMetadataProperty property) {
		Deprecation deprecation = (property.getDeprecation() != null) ? property.getDeprecation() : new Deprecation();
		this.out.append("|`").append(property.getId()).append("` |");
		if (deprecation.getReplacement() != null) {
			this.out.append("`").append(deprecation.getReplacement()).append("`");
		}
		this.out.append(" |");
		if (deprecation.getReason() != null) {
			this.out.append(getFirstSentence(deprecation.getReason()));
		}
		this.out.append(System.lineSeparator());
	}

	private String getFirstSentence(String text) {
		int dot = text.indexOf('.');
		if (dot != -1) {
			BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
			breakIterator.setText(text);
			String sentence = text.substring(breakIterator.first(), breakIterator.next()).trim();
			return removeSpaceBetweenLine(sentence);
		}
		else {
			String[] lines = text.split(System.lineSeparator());
			return lines[0].trim();
		}
	}

	private static String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(System.lineSeparator());
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line.trim()).append(" ");
		}
		return sb.toString().trim();
	}

	private List<Difference> sortProperties(List<Difference> properties,
			Function<Difference, ConfigurationMetadataProperty> property) {
		List<Difference> sorted = new ArrayList<>(properties);
		sorted.sort((o1, o2) -> property.apply(o1).getId().compareTo(property.apply(o2).getId()));
		return sorted;
	}

	private static String defaultValueToString(Object defaultValue) {
		if (defaultValue instanceof Object[]) {
			return Stream.of((Object[]) defaultValue).map(Object::toString).collect(Collectors.joining(", "));
		}
		else {
			return defaultValue.toString();
		}
	}

	@Override
	public void close() {
		this.out.close();
	}

}
