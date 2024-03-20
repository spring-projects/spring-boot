/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;

/**
 * Writes a {@link Changelog} using asciidoc markup.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class ChangelogWriter implements AutoCloseable {

	private static final Comparator<ConfigurationMetadataProperty> COMPARING_ID = Comparator
		.comparing(ConfigurationMetadataProperty::getId);

	private final PrintWriter out;

	ChangelogWriter(File out) throws IOException {
		this(new FileWriter(out));
	}

	ChangelogWriter(Writer out) {
		this.out = new PrintWriter(out);
	}

	void write(Changelog changelog) {
		String oldVersionNumber = changelog.oldVersionNumber();
		String newVersionNumber = changelog.newVersionNumber();
		Map<DifferenceType, List<Difference>> differencesByType = collateByType(changelog);
		write("Configuration property changes between `%s` and `%s`%n", oldVersionNumber, newVersionNumber);
		write("%n%n%n== Deprecated in %s%n%n", newVersionNumber);
		writeDeprecated(differencesByType.get(DifferenceType.DEPRECATED));
		write("%n%n%n== Added in %s%n%n", newVersionNumber);
		writeAdded(differencesByType.get(DifferenceType.ADDED));
		write("%n%n%n== Removed in %s%n%n", newVersionNumber);
		writeRemoved(differencesByType.get(DifferenceType.DELETED), differencesByType.get(DifferenceType.DEPRECATED));
	}

	private Map<DifferenceType, List<Difference>> collateByType(Changelog differences) {
		Map<DifferenceType, List<Difference>> byType = new HashMap<>();
		for (DifferenceType type : DifferenceType.values()) {
			byType.put(type, new ArrayList<>());
		}
		for (Difference difference : differences.differences()) {
			byType.get(difference.type()).add(difference);
		}
		return byType;
	}

	private void writeDeprecated(List<Difference> differences) {
		List<Difference> rows = sortProperties(differences, Difference::newProperty).stream()
			.filter(this::isDeprecatedInRelease)
			.toList();
		writeTable("| Key | Replacement | Reason", rows, this::writeDeprecated);
	}

	private void writeDeprecated(Difference difference) {
		writeDeprecatedPropertyRow(difference.newProperty());
	}

	private void writeAdded(List<Difference> differences) {
		List<Difference> rows = sortProperties(differences, Difference::newProperty);
		writeTable("| Key | Default value | Description", rows, this::writeAdded);
	}

	private void writeAdded(Difference difference) {
		writeRegularPropertyRow(difference.newProperty());
	}

	private void writeRemoved(List<Difference> deleted, List<Difference> deprecated) {
		List<Difference> rows = getRemoved(deleted, deprecated);
		writeTable("| Key | Replacement | Reason", rows, this::writeRemoved);
	}

	private List<Difference> getRemoved(List<Difference> deleted, List<Difference> deprecated) {
		List<Difference> result = new ArrayList<>(deleted);
		deprecated.stream().filter(Predicate.not(this::isDeprecatedInRelease)).forEach(result::remove);
		return sortProperties(result,
				(difference) -> getFirstNonNull(difference, Difference::oldProperty, Difference::newProperty));
	}

	private void writeRemoved(Difference difference) {
		writeDeprecatedPropertyRow(getFirstNonNull(difference, Difference::newProperty, Difference::oldProperty));
	}

	private List<Difference> sortProperties(List<Difference> differences,
			Function<Difference, ConfigurationMetadataProperty> extractor) {
		return differences.stream().sorted(Comparator.comparing(extractor, COMPARING_ID)).toList();
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private <T, P> P getFirstNonNull(T t, Function<T, P>... extractors) {
		return Stream.of(extractors)
			.map((extractor) -> extractor.apply(t))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	private void writeTable(String header, List<Difference> rows, Consumer<Difference> action) {
		if (rows.isEmpty()) {
			write("_None_.%n");
		}
		else {
			writeTableBreak();
			write(header + "%n%n");
			for (Iterator<Difference> iterator = rows.iterator(); iterator.hasNext();) {
				action.accept(iterator.next());
				write((!iterator.hasNext()) ? null : "%n");
			}
			writeTableBreak();
		}
	}

	private void writeTableBreak() {
		write("|======================%n");
	}

	private void writeRegularPropertyRow(ConfigurationMetadataProperty property) {
		writeCell(monospace(property.getId()));
		writeCell(monospace(asString(property.getDefaultValue())));
		writeCell(property.getShortDescription());
	}

	private void writeDeprecatedPropertyRow(ConfigurationMetadataProperty property) {
		Deprecation deprecation = (property.getDeprecation() != null) ? property.getDeprecation() : new Deprecation();
		writeCell(monospace(property.getId()));
		writeCell(monospace(deprecation.getReplacement()));
		writeCell(getFirstSentence(deprecation.getReason()));
	}

	private String getFirstSentence(String text) {
		if (text == null) {
			return null;
		}
		int dot = text.indexOf('.');
		if (dot != -1) {
			BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
			breakIterator.setText(text);
			String sentence = text.substring(breakIterator.first(), breakIterator.next()).trim();
			return removeSpaceBetweenLine(sentence);
		}
		String[] lines = text.split(System.lineSeparator());
		return lines[0].trim();
	}

	private String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(System.lineSeparator());
		return Arrays.stream(lines).map(String::trim).collect(Collectors.joining(" "));
	}

	private boolean isDeprecatedInRelease(Difference difference) {
		Deprecation deprecation = difference.newProperty().getDeprecation();
		return (deprecation != null) && (deprecation.getLevel() != Deprecation.Level.ERROR);
	}

	private String monospace(String value) {
		return (value != null) ? "`%s`".formatted(value) : null;
	}

	private void writeCell(String content) {
		if (content == null) {
			write("|%n");
		}
		else {
			String escaped = escapeForTableCell(content);
			write("| %s%n".formatted(escaped));
		}
	}

	private String escapeForTableCell(String content) {
		return content.replace("|", "\\|");
	}

	private void write(String format, Object... args) {
		if (format != null) {
			Object[] strings = Arrays.stream(args).map(this::asString).toArray();
			this.out.append(format.formatted(strings));
		}
	}

	private String asString(Object value) {
		if (value instanceof Object[] array) {
			return Stream.of(array).map(this::asString).collect(Collectors.joining(", "));
		}
		return (value != null) ? value.toString() : null;
	}

	@Override
	public void close() {
		this.out.close();
	}

}
