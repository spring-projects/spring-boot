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
import java.util.Set;
import java.util.TreeSet;

/**
 * Asciidoctor table listing configuration properties sharing to a common theme.
 *
 * @author Brian Clozel
 */
class ConfigurationTable {

	private final String id;

	private final Set<ConfigurationTableEntry> entries;

	ConfigurationTable(String id) {
		this.id = id;
		this.entries = new TreeSet<>();
	}

	String getId() {
		return this.id;
	}

	void addEntry(ConfigurationTableEntry... entries) {
		this.entries.addAll(Arrays.asList(entries));
	}

	String toAsciidocTable() {
		AsciidocBuilder builder = new AsciidocBuilder();
		builder.appendln("[cols=\"2,1,1\", options=\"header\"]");
		builder.appendln("|===");
		builder.appendln("|Key|Default Value|Description");
		builder.appendln();
		this.entries.forEach((entry) -> {
			entry.write(builder);
			builder.appendln();
		});
		return builder.appendln("|===").toString();
	}

}
