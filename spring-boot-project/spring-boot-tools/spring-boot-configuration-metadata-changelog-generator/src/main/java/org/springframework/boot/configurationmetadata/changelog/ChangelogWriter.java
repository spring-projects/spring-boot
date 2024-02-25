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

	/**
	 * Constructs a new ChangelogWriter object with the specified output file.
	 * @param out the output file to write the changelog to
	 * @throws IOException if an I/O error occurs while opening or writing to the output
	 * file
	 */
	ChangelogWriter(File out) throws IOException {
		this(new FileWriter(out));
	}

	/**
	 * Constructs a new ChangelogWriter with the specified output writer.
	 * @param out the output writer to write the changelog to
	 */
	ChangelogWriter(Writer out) {
		this.out = new PrintWriter(out);
	}

	/**
	 * Writes the changelog information to the output.
	 * @param changelog the Changelog object containing the information to be written
	 */
	void write(Changelog changelog) {
		String oldVersionNumber = changelog.oldVersionNumber();
		String newVersionNumber = changelog.newVersionNumber();
		Map<DifferenceType, List<Difference>> differencesByType = collateByType(changelog);
		write("Configuration property changes between `%s` and `%s`%n", oldVersionNumber, newVersionNumber);
		write("%n%n%n== Deprecated in %s%n", newVersionNumber);
		writeDeprecated(differencesByType.get(DifferenceType.DEPRECATED));
		write("%n%n%n== Added in %s%n", newVersionNumber);
		writeAdded(differencesByType.get(DifferenceType.ADDED));
		write("%n%n%n== Removed in %s%n", newVersionNumber);
		writeRemoved(differencesByType.get(DifferenceType.DELETED), differencesByType.get(DifferenceType.DEPRECATED));
	}

	/**
	 * Collates the differences in the given Changelog object by their type.
	 * @param differences the Changelog object containing the differences
	 * @return a Map object where the keys are DifferenceType and the values are lists of
	 * Difference objects
	 */
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

	/**
	 * Writes the deprecated properties to the table in the changelog.
	 * @param differences the list of differences containing deprecated properties
	 */
	private void writeDeprecated(List<Difference> differences) {
		List<Difference> rows = sortProperties(differences, Difference::newProperty).stream()
			.filter(this::isDeprecatedInRelease)
			.toList();
		writeTable("| Key | Replacement | Reason", rows, this::writeDeprecated);
	}

	/**
	 * Writes the deprecated property row for the given difference.
	 * @param difference the difference object containing the deprecated property
	 */
	private void writeDeprecated(Difference difference) {
		writeDeprecatedPropertyRow(difference.newProperty());
	}

	/**
	 * Writes the added properties to the changelog table.
	 * @param differences the list of differences containing the added properties
	 */
	private void writeAdded(List<Difference> differences) {
		List<Difference> rows = sortProperties(differences, Difference::newProperty);
		writeTable("| Key | Default value | Description", rows, this::writeAdded);
	}

	/**
	 * Writes the added property to the changelog.
	 * @param difference the Difference object representing the added property
	 */
	private void writeAdded(Difference difference) {
		writeRegularPropertyRow(difference.newProperty());
	}

	/**
	 * Writes the removed differences to the changelog table.
	 * @param deleted the list of deleted differences
	 * @param deprecated the list of deprecated differences
	 */
	private void writeRemoved(List<Difference> deleted, List<Difference> deprecated) {
		List<Difference> rows = getRemoved(deleted, deprecated);
		writeTable("| Key | Replacement | Reason", rows, this::writeRemoved);
	}

	/**
	 * Returns a list of removed differences by filtering out the deprecated differences
	 * from the deleted differences.
	 * @param deleted the list of deleted differences
	 * @param deprecated the list of deprecated differences
	 * @return a list of removed differences
	 */
	private List<Difference> getRemoved(List<Difference> deleted, List<Difference> deprecated) {
		List<Difference> result = new ArrayList<>(deleted);
		deprecated.stream().filter(Predicate.not(this::isDeprecatedInRelease)).forEach(result::remove);
		return sortProperties(result,
				(difference) -> getFirstNonNull(difference, Difference::oldProperty, Difference::newProperty));
	}

	/**
	 * Writes the removed property row for the given difference.
	 * @param difference the difference object containing the removed property
	 */
	private void writeRemoved(Difference difference) {
		writeDeprecatedPropertyRow(getFirstNonNull(difference, Difference::newProperty, Difference::oldProperty));
	}

	/**
	 * Sorts the list of differences based on the provided extractor function.
	 * @param differences the list of differences to be sorted
	 * @param extractor the function used to extract the ConfigurationMetadataProperty
	 * from each Difference object
	 * @return the sorted list of differences
	 */
	private List<Difference> sortProperties(List<Difference> differences,
			Function<Difference, ConfigurationMetadataProperty> extractor) {
		return differences.stream().sorted(Comparator.comparing(extractor, COMPARING_ID)).toList();
	}

	/**
	 * Returns the first non-null result obtained by applying the given extractors to the
	 * specified object.
	 * @param <T> the type of the object to apply the extractors to
	 * @param <P> the type of the result obtained by applying the extractors
	 * @param t the object to apply the extractors to
	 * @param extractors the extractors to apply to the object
	 * @return the first non-null result obtained by applying the extractors, or null if
	 * all results are null
	 * @throws NullPointerException if any of the extractors is null
	 *
	 * @since version 1.0
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	private <T, P> P getFirstNonNull(T t, Function<T, P>... extractors) {
		return Stream.of(extractors)
			.map((extractor) -> extractor.apply(t))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Writes a table with the given header and rows using the provided action to process
	 * each row. If the rows list is empty, it writes "_None_".
	 * @param header The header of the table.
	 * @param rows The list of rows to be written in the table.
	 * @param action The action to be performed on each row.
	 */
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

	/**
	 * Writes a table break to the output file.
	 *
	 * The table break consists of a line of equal signs to separate table rows.
	 * @param none
	 * @return void
	 */
	private void writeTableBreak() {
		write("|======================%n");
	}

	/**
	 * Writes a regular property row to the changelog.
	 * @param property the ConfigurationMetadataProperty object representing the property
	 */
	private void writeRegularPropertyRow(ConfigurationMetadataProperty property) {
		writeCell(monospace(property.getId()));
		writeCell(monospace(asString(property.getDefaultValue())));
		writeCell(property.getShortDescription());
	}

	/**
	 * Writes a row for a deprecated property in the configuration metadata.
	 * @param property the ConfigurationMetadataProperty object representing the
	 * deprecated property
	 */
	private void writeDeprecatedPropertyRow(ConfigurationMetadataProperty property) {
		Deprecation deprecation = (property.getDeprecation() != null) ? property.getDeprecation() : new Deprecation();
		writeCell(monospace(property.getId()));
		writeCell(monospace(deprecation.getReplacement()));
		writeCell(getFirstSentence(deprecation.getReason()));
	}

	/**
	 * Returns the first sentence of the given text. If the text is null, null is
	 * returned. If the text contains a period ('.'), the first sentence is extracted
	 * using a BreakIterator. If the text does not contain a period, the first line of the
	 * text is returned.
	 * @param text the text from which to extract the first sentence
	 * @return the first sentence of the text, or the first line if no period is found, or
	 * null if the text is null
	 */
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

	/**
	 * Removes the spaces between lines in the given text.
	 * @param text the text to remove spaces from
	 * @return the text with spaces between lines removed
	 */
	private String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(System.lineSeparator());
		return Arrays.stream(lines).map(String::trim).collect(Collectors.joining(" "));
	}

	/**
	 * Checks if a property is deprecated in a release.
	 * @param difference the Difference object representing the property difference
	 * @return true if the property is deprecated in the release, false otherwise
	 */
	private boolean isDeprecatedInRelease(Difference difference) {
		Deprecation deprecation = difference.newProperty().getDeprecation();
		return (deprecation != null) && (deprecation.getLevel() != Deprecation.Level.ERROR);
	}

	/**
	 * Formats the given value as monospace by enclosing it in backticks.
	 * @param value the value to be formatted as monospace
	 * @return the monospace formatted value, or null if the input value is null
	 */
	private String monospace(String value) {
		return (value != null) ? "`%s`".formatted(value) : null;
	}

	/**
	 * Writes the content to a cell in the changelog table. If the content is null, a new
	 * line is written. Otherwise, the content is escaped and written with a preceding
	 * pipe character.
	 * @param content the content to be written to the cell
	 */
	private void writeCell(String content) {
		if (content == null) {
			write("|%n");
		}
		else {
			String escaped = escapeForTableCell(content);
			write("| %s%n".formatted(escaped));
		}
	}

	/**
	 * Escapes the given content for a table cell in a ChangelogWriter. Replaces the "|"
	 * character with "\\|".
	 * @param content the content to be escaped
	 * @return the escaped content
	 */
	private String escapeForTableCell(String content) {
		return content.replace("|", "\\|");
	}

	/**
	 * Writes the formatted string to the output stream.
	 * @param format the format string
	 * @param args the arguments to be formatted
	 */
	private void write(String format, Object... args) {
		if (format != null) {
			Object[] strings = Arrays.stream(args).map(this::asString).toArray();
			this.out.append(format.formatted(strings));
		}
	}

	/**
	 * Converts an object to its string representation. If the object is an array, it
	 * converts each element to its string representation and joins them with a comma.
	 * @param value the object to convert
	 * @return the string representation of the object, or null if the object is null
	 */
	private String asString(Object value) {
		if (value instanceof Object[] array) {
			return Stream.of(array).map(this::asString).collect(Collectors.joining(", "));
		}
		return (value != null) ? value.toString() : null;
	}

	/**
	 * Closes the output stream.
	 */
	@Override
	public void close() {
		this.out.close();
	}

}
