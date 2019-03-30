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
class SingleKeyEntry extends AbstractConfigurationEntry {

	private String defaultValue;

	private String description;

	SingleKeyEntry(ConfigurationMetadataProperty property) {

		this.key = property.getId();
		if (property.getType() != null
				&& property.getType().startsWith("java.util.Map")) {
			this.key += ".*";
		}

		this.description = property.getDescription();

		if (property.getDefaultValue() != null) {
			if (property.getDefaultValue().getClass().isArray()) {
				this.defaultValue = Arrays.stream((Object[]) property.getDefaultValue())
						.map(Object::toString).collect(Collectors.joining("," + NEWLINE));
			}
			else {
				this.defaultValue = property.getDefaultValue().toString();
			}
		}
	}

	@Override
	public void writeAsciidoc(StringBuilder builder) {
		builder.append("|`+").append(this.key).append("+`").append(NEWLINE);
		String defaultValue = processDefaultValue();
		if (!defaultValue.isEmpty()) {
			builder.append("|`+").append(defaultValue).append("+`").append(NEWLINE);
		}
		else {
			builder.append("|").append(NEWLINE);
		}
		if (this.description != null) {
			builder.append("|+++").append(this.description).append("+++");
		}
		else {
			builder.append("|");
		}
		builder.append(NEWLINE);
	}

	private String processDefaultValue() {
		if (this.defaultValue != null && !this.defaultValue.isEmpty()) {
			return this.defaultValue.replace("\\", "\\\\").replace("|",
					"{vbar}" + NEWLINE);
		}
		return "";
	}

}
