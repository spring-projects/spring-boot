/*
 * Copyright 2012-2020 the original author or authors.
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
 * Table entry containing a single configuration property.
 *
 * @author Brian Clozel
 */
class SingleConfigurationTableEntry extends ConfigurationTableEntry {

	private final String description;

	private final String defaultValue;

	private final String anchor;

	SingleConfigurationTableEntry(ConfigurationProperty property) {
		this.key = property.getName();
		this.anchor = this.key;
		if (property.getType() != null && property.getType().startsWith("java.util.Map")) {
			this.key += ".*";
		}
		this.description = property.getDescription();
		this.defaultValue = getDefaultValue(property.getDefaultValue());
	}

	private String getDefaultValue(Object defaultValue) {
		if (defaultValue == null) {
			return null;
		}
		if (defaultValue.getClass().isArray()) {
			return Arrays.stream((Object[]) defaultValue).map(Object::toString)
					.collect(Collectors.joining("," + System.lineSeparator()));
		}
		return defaultValue.toString();
	}

	@Override
	void write(AsciidocBuilder builder) {
		builder.appendln("|[[" + this.anchor + "]]<<" + this.anchor + ",`+", this.key, "+`>>");
		writeDefaultValue(builder);
		writeDescription(builder);
		builder.appendln();
	}

	private void writeDefaultValue(AsciidocBuilder builder) {
		String defaultValue = (this.defaultValue != null) ? this.defaultValue : "";
		if (defaultValue.isEmpty()) {
			builder.appendln("|");
		}
		else {
			defaultValue = defaultValue.replace("\\", "\\\\").replace("|", "\\|");
			builder.appendln("|`+", defaultValue, "+`");
		}
	}

	private void writeDescription(AsciidocBuilder builder) {
		if (this.description == null || this.description.isEmpty()) {
			builder.append("|");
		}
		else {
			String cleanedDescription = this.description.replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;");
			builder.append("|+++", cleanedDescription, "+++");
		}
	}

}
