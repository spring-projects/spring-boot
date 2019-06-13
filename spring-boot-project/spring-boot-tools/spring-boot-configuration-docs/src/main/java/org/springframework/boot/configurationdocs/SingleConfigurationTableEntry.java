/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.configurationdocs;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

/**
 * Table entry containing a single configuration property.
 *
 * @author Brian Clozel
 */
class SingleConfigurationTableEntry extends ConfigurationTableEntry {

	private final String description;

	private final String defaultValue;

	SingleConfigurationTableEntry(ConfigurationMetadataProperty property) {
		this.key = property.getId();
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
	public void write(AsciidocBuilder builder) {
		builder.appendln("|`+", this.key, "+`");
		writeDefaultValue(builder);
		writeDescription(builder);
		builder.appendln();
	}

	private void writeDefaultValue(AsciidocBuilder builder) {
		String defaultValue = (this.defaultValue != null) ? this.defaultValue : "";
		defaultValue = defaultValue.replace("\\", "\\\\").replace("|", "{vbar}" + System.lineSeparator());
		if (defaultValue.isEmpty()) {
			builder.appendln("|");
		}
		else {
			builder.appendln("|`+", defaultValue, "+`");
		}
	}

	private void writeDescription(AsciidocBuilder builder) {
		if (this.description == null || this.description.isEmpty()) {
			builder.append("|");
		}
		else {
			builder.append("|+++", this.description, "+++");
		}
	}

}
