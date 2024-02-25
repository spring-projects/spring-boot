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
 * Simple builder to help construct Asciidoc markup.
 *
 * @author Phillip Webb
 */
class Asciidoc {

	private final StringBuilder content;

	/**
     * Constructor for the Asciidoc class.
     */
    Asciidoc() {
		this.content = new StringBuilder();
	}

	/**
     * Appends the given items to the Asciidoc content with hard line breaks.
     * 
     * @param items the items to be appended
     * @return the updated Asciidoc content
     */
    Asciidoc appendWithHardLineBreaks(Object... items) {
		for (Object item : items) {
			appendln("`+", item, "+` +");
		}
		return this;
	}

	/**
     * Appends the specified objects to the current Asciidoc content and adds a new line.
     *
     * @param items the objects to be appended
     * @return the updated Asciidoc content with the appended objects and a new line
     */
    Asciidoc appendln(Object... items) {
		return append(items).newLine();
	}

	/**
     * Appends the specified items to the content of this Asciidoc object.
     * 
     * @param items the items to be appended
     * @return the updated Asciidoc object
     */
    Asciidoc append(Object... items) {
		for (Object item : items) {
			this.content.append(item);
		}
		return this;
	}

	/**
     * Appends a new line to the Asciidoc content.
     *
     * @return the Asciidoc object with the new line appended
     */
    Asciidoc newLine() {
		return append(System.lineSeparator());
	}

	/**
     * Returns a string representation of the content of this Asciidoc object.
     *
     * @return a string representation of the content
     */
    @Override
	public String toString() {
		return this.content.toString();
	}

}
