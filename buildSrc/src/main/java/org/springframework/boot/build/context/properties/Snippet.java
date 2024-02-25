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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A configuration properties snippet.
 *
 * @author Brian Clozed
 * @author Phillip Webb
 */
class Snippet {

	private final String anchor;

	private final String title;

	private final Set<String> prefixes;

	private final Map<String, String> overrides;

	/**
     * Creates a new Snippet with the specified anchor and title, and applies the provided configuration.
     * 
     * @param anchor the anchor for the Snippet
     * @param title the title for the Snippet
     * @param config the configuration to be applied to the Snippet
     */
    Snippet(String anchor, String title, Consumer<Config> config) {
		Set<String> prefixes = new LinkedHashSet<>();
		Map<String, String> overrides = new LinkedHashMap<>();
		if (config != null) {
			config.accept(new Config() {

				@Override
				public void accept(String prefix) {
					prefixes.add(prefix);
				}

				@Override
				public void accept(String prefix, String description) {
					overrides.put(prefix, description);
				}

			});
		}
		this.anchor = anchor;
		this.title = title;
		this.prefixes = prefixes;
		this.overrides = overrides;
	}

	/**
     * Returns the anchor of the Snippet.
     *
     * @return the anchor of the Snippet
     */
    String getAnchor() {
		return this.anchor;
	}

	/**
     * Returns the title of the Snippet.
     *
     * @return the title of the Snippet
     */
    String getTitle() {
		return this.title;
	}

	/**
     * Iterates over each prefix in the prefixes list and applies the specified action to it.
     *
     * @param action the action to be applied to each prefix
     */
    void forEachPrefix(Consumer<String> action) {
		this.prefixes.forEach(action);
	}

	/**
     * Iterates over each entry in the overrides map and applies the specified action to each entry.
     * 
     * @param action the action to be applied to each entry in the overrides map
     */
    void forEachOverride(BiConsumer<String, String> action) {
		this.overrides.forEach(action);
	}

	/**
	 * Callback to configure the snippet.
	 */
	interface Config {

		/**
		 * Accept the given prefix using the meta-data description.
		 * @param prefix the prefix to accept
		 */
		void accept(String prefix);

		/**
		 * Accept the given prefix with a defined description.
		 * @param prefix the prefix to accept
		 * @param description the description to use
		 */
		void accept(String prefix, String description);

	}

}
