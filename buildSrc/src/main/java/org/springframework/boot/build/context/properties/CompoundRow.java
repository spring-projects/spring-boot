/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.context.properties;

import java.util.Set;
import java.util.TreeSet;

/**
 * Table row regrouping a list of configuration properties sharing the same description.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class CompoundRow extends Row {

	private final Set<String> propertyNames;

	private final String description;

	CompoundRow(Snippet snippet, String prefix, String description) {
		super(snippet, prefix);
		this.description = description;
		this.propertyNames = new TreeSet<>();
	}

	void addProperty(ConfigurationProperty property) {
		this.propertyNames.add(property.getDisplayName());
	}

	boolean isEmpty() {
		return this.propertyNames.isEmpty();
	}

	@Override
	void write(Asciidoc asciidoc) {
		asciidoc.append("|");
		asciidoc.append("[[" + getAnchor() + "]]");
		asciidoc.append("xref:#" + getAnchor() + "[");
		this.propertyNames.forEach(asciidoc::appendWithHardLineBreaks);
		asciidoc.appendln("]");
		asciidoc.appendln("|+++", this.description, "+++");
		asciidoc.appendln("|");
	}

}
