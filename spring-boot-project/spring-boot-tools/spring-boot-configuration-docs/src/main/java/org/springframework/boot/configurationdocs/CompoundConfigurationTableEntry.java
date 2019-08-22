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

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

/**
 * Table entry regrouping a list of configuration properties sharing the same description.
 *
 * @author Brian Clozel
 */
class CompoundConfigurationTableEntry extends ConfigurationTableEntry {

	private Set<String> configurationKeys;

	private String description;

	CompoundConfigurationTableEntry(String key, String description) {
		this.key = key;
		this.description = description;
		this.configurationKeys = new TreeSet<>();
	}

	void addConfigurationKeys(ConfigurationMetadataProperty... properties) {
		Stream.of(properties).map(ConfigurationMetadataProperty::getId).forEach(this.configurationKeys::add);
	}

	@Override
	void write(AsciidocBuilder builder) {
		builder.append("|");
		this.configurationKeys.forEach(builder::appendKey);
		builder.newLine().appendln("|").appendln("|+++", this.description, "+++");
	}

}
