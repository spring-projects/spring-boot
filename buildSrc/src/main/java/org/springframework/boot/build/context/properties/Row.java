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
