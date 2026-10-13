/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.context.properties;

/**
 * Abstract class for rows in {@link Table}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
abstract class Row implements Comparable<Row> {

	private final Snippet snippet;

	private final String id;

	protected Row(Snippet snippet, String id) {
		this.snippet = snippet;
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Row other = (Row) obj;
		return this.id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public int compareTo(Row other) {
		return this.id.compareTo(other.id);
	}

	String getAnchor() {
		return this.snippet.getAnchor() + "." + this.id;
	}

	abstract void write(Asciidoc asciidoc);

}
