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

/**
 * Simple builder to help construct Asciidoc markup.
 *
 * @author Phillip Webb
 */
class AsciidocBuilder {

	private final StringBuilder content;

	AsciidocBuilder() {
		this.content = new StringBuilder();
	}

	AsciidocBuilder appendKey(Object... items) {
		for (Object item : items) {
			appendln("`+", item, "+` +");
		}
		return this;
	}

	AsciidocBuilder newLine() {
		return append(System.lineSeparator());
	}

	AsciidocBuilder appendln(Object... items) {
		return append(items).newLine();
	}

	AsciidocBuilder append(Object... items) {
		for (Object item : items) {
			this.content.append(item);
		}
		return this;
	}

	@Override
	public String toString() {
		return this.content.toString();
	}

}
