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
import java.util.Set;
import java.util.TreeSet;

/**
 * Asciidoctor table listing configuration properties sharing to a common theme.
 *
 * @author Brian Clozel
 */
class ConfigurationTable {

	private static final String NEWLINE = System.lineSeparator();

	private final String id;

	private final Set<AbstractConfigurationEntry> entries;

	ConfigurationTable(String id) {
		this.id = id;
		this.entries = new TreeSet<>();
	}

	public String getId() {
		return this.id;
	}

	void addEntry(AbstractConfigurationEntry... entries) {
		this.entries.addAll(Arrays.asList(entries));
	}

	String toAsciidocTable() {
		final StringBuilder builder = new StringBuilder();
		builder.append("[cols=\"1,1,2\", options=\"header\"]").append(NEWLINE);
		builder.append("|===").append(NEWLINE).append("|Key|Default Value|Description")
				.append(NEWLINE).append(NEWLINE);
		this.entries.forEach((entry) -> {
			entry.writeAsciidoc(builder);
			builder.append(NEWLINE);
		});
		return builder.append("|===").append(NEWLINE).toString();
	}

}
