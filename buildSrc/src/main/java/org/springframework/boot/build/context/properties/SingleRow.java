/*
 * Copyright 2012-present the original author or authors.
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

import org.springframework.boot.build.context.properties.ConfigurationProperty.Deprecation;

/**
 * Table row containing a single configuration property.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class SingleRow extends Row {

	private final Snippets snippets;

	private final String defaultValue;

	private final ConfigurationProperty property;

	SingleRow(Snippet snippet, ConfigurationProperty property) {
		this(null, snippet, property);
	}

	SingleRow(Snippets snippets, Snippet snippet, ConfigurationProperty property) {
		super(snippet, property.getName());
		this.snippets = snippets;
		this.defaultValue = getDefaultValue(property.getDefaultValue());
		this.property = property;
	}

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

	@Override
	void write(Asciidoc asciidoc) {
		asciidoc.append("|");
		asciidoc.append("[[" + getAnchor() + "]]");
		asciidoc.appendln("xref:#" + getAnchor() + "[`+", this.property.getDisplayName(), "+`]");
		writeDescription(asciidoc);
		writeDefaultValue(asciidoc);
	}

	private void writeDescription(Asciidoc builder) {
		builder.append("|");
		if (this.property.isDeprecated()) {
			Deprecation deprecation = this.property.getDeprecation();
			String replacement = (deprecation != null) ? deprecation.replacement() : null;
			String reason = (deprecation != null) ? deprecation.reason() : null;
			if (replacement != null && !replacement.isEmpty()) {
				String xref = (this.snippets != null) ? this.snippets.findXref(deprecation.replacement()) : null;
				if (xref != null) {
					builder.append("Replaced by xref:" + xref + "[`+" + deprecation.replacement() + "+`]");
				}
				else {
					builder.append("Replaced by `+" + deprecation.replacement() + "+`");
				}
			}
			else if (reason != null && !reason.isEmpty()) {
				builder.append("+++", clean(reason), "+++");
			}
		}
		else {
			String description = this.property.getDescription();
			if (description != null && !description.isEmpty()) {
				builder.append("+++", clean(description), "+++");
			}
		}
		builder.appendln();
	}

	private String clean(String text) {
		return text.replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;");
	}

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
