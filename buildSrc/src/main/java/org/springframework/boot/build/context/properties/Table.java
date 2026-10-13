/*
 * Copyright 2012-present the original author or authors.
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

	boolean isEmpty() {
		return this.rows.isEmpty();
	}

}
