/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.context.properties;

/**
 * Simple builder to help construct Asciidoc markup.
 *
 * @author Phillip Webb
 */
class Asciidoc {

	private final StringBuilder content;

	Asciidoc() {
		this.content = new StringBuilder();
	}

	Asciidoc appendWithHardLineBreaks(Object... items) {
		for (Object item : items) {
			appendln("`+", item, "+` +");
		}
		return this;
	}

	Asciidoc appendln(Object... items) {
		return append(items).newLine();
	}

	Asciidoc append(Object... items) {
		for (Object item : items) {
			this.content.append(item);
		}
		return this;
	}

	Asciidoc newLine() {
		return append(System.lineSeparator());
	}

	@Override
	public String toString() {
		return this.content.toString();
	}

}
