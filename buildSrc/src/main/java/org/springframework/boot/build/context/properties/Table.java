/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Set;
import java.util.TreeSet;

/**
 * Asciidoctor table listing configuration properties sharing to a common theme.
 *
 * @author Brian Clozel
 */
class Table {

	private final Set<Row> rows = new TreeSet<>();

	void addRow(Row row) {
		this.rows.add(row);
	}

	void write(Asciidoc asciidoc) {
		asciidoc.appendln("[cols=\"4,3,3\", options=\"header\"]");
		asciidoc.appendln("|===");
		asciidoc.appendln("|Name|Description|Default Value");
		asciidoc.appendln();
		this.rows.forEach((entry) -> {
			entry.write(asciidoc);
			asciidoc.appendln();
		});
		asciidoc.appendln("|===");
	}

}
