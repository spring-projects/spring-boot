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

/**
 * Abstract class for rows in {@link Table}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
abstract class Row implements Comparable<Row> {

	private final Snippet snippet;

	private final String id;

	/**
	 * Constructs a new Row object with the given Snippet and id.
	 * @param snippet the Snippet object associated with the Row
	 * @param id the unique identifier for the Row
	 */
	protected Row(Snippet snippet, String id) {
		this.snippet = snippet;
		this.id = id;
	}

	/**
	 * Compares this Row object to the specified object. The result is true if and only if
	 * the argument is not null and is a Row object that represents the same row as this
	 * object.
	 * @param obj the object to compare this Row against
	 * @return true if the given object represents a Row equivalent to this Row, false
	 * otherwise
	 */
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

	/**
	 * Returns the hash code value for this Row object.
	 * @return the hash code value for this Row object
	 */
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	/**
	 * Compares this Row object with the specified Row object for order. Returns a
	 * negative integer, zero, or a positive integer as this object is less than, equal
	 * to, or greater than the specified object.
	 * @param other the Row object to be compared
	 * @return a negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object
	 */
	@Override
	public int compareTo(Row other) {
		return this.id.compareTo(other.id);
	}

	/**
	 * Returns the anchor of the Row. The anchor is obtained by concatenating the anchor
	 * of the snippet and the id of the Row.
	 * @return the anchor of the Row
	 */
	String getAnchor() {
		return this.snippet.getAnchor() + "." + this.id;
	}

	/**
	 * Writes the given Asciidoc content to the row.
	 * @param asciidoc the Asciidoc content to be written
	 */
	abstract void write(Asciidoc asciidoc);

}
