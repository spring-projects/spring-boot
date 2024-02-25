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

package org.springframework.boot.build.context.properties;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Table row containing a single configuration property.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class SingleRow extends Row {

	private final String displayName;

	private final String description;

	private final String defaultValue;

	/**
	 * Constructs a new SingleRow object with the given Snippet and ConfigurationProperty.
	 * @param snippet the Snippet object to be used
	 * @param property the ConfigurationProperty object to be used
	 */
	SingleRow(Snippet snippet, ConfigurationProperty property) {
		super(snippet, property.getName());
		this.displayName = property.getDisplayName();
		this.description = property.getDescription();
		this.defaultValue = getDefaultValue(property.getDefaultValue());
	}

	/**
	 * Returns the default value as a string representation.
	 * @param defaultValue the default value to be converted to a string
	 * @return the string representation of the default value, or null if the default
	 * value is null
	 */
	private String getDefaultValue(Object defaultValue) {
		if (defaultValue == null) {
			return null;
		}
		if (defaultValue.getClass().isArray()) {
			return Arrays.stream((Object[]) defaultValue)
				.map(Object::toString)
				.collect(Collectors.joining("," + System.lineSeparator()));
		}
		return defaultValue.toString();
	}

	/**
	 * Writes the given Asciidoc object with the information of this SingleRow object.
	 * @param asciidoc the Asciidoc object to write to
	 */
	@Override
	void write(Asciidoc asciidoc) {
		asciidoc.append("|");
		asciidoc.append("[[" + getAnchor() + "]]");
		asciidoc.appendln("<<" + getAnchor() + ",`+", this.displayName, "+`>>");
		writeDescription(asciidoc);
		writeDefaultValue(asciidoc);
	}

	/**
	 * Writes the description of the SingleRow object to the provided Asciidoc builder. If
	 * the description is null or empty, it appends an empty line to the builder.
	 * Otherwise, it cleans the description by replacing "|" with "\\|", "<" with "&lt;",
	 * and ">" with "&gt;", and appends the cleaned description to the builder enclosed in
	 * "+++...+++".
	 * @param builder the Asciidoc builder to write the description to
	 */
	private void writeDescription(Asciidoc builder) {
		if (this.description == null || this.description.isEmpty()) {
			builder.appendln("|");
		}
		else {
			String cleanedDescription = this.description.replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;");
			builder.appendln("|+++", cleanedDescription, "+++");
		}
	}

	/**
	 * Writes the default value of the SingleRow object to the given Asciidoc builder. If
	 * the default value is null or empty, it writes an empty cell. Otherwise, it writes
	 * the default value enclosed in backticks and surrounded by '+' signs. Any
	 * backslashes or pipe characters in the default value are escaped with a backslash.
	 * @param builder the Asciidoc builder to write the default value to
	 */
	private void writeDefaultValue(Asciidoc builder) {
		String defaultValue = (this.defaultValue != null) ? this.defaultValue : "";
		if (defaultValue.isEmpty()) {
			builder.appendln("|");
		}
		else {
			defaultValue = defaultValue.replace("\\", "\\\\").replace("|", "\\|");
			builder.appendln("|`+", defaultValue, "+`");
		}
	}

}
