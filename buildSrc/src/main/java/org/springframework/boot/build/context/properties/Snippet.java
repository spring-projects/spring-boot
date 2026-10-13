/*
 * Copyright 2012-present the original author or authors.
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

	String getAnchor() {
		return this.anchor;
	}

	String getTitle() {
		return this.title;
	}

	Set<String> getPrefixes() {
		return this.prefixes;
	}

	Map<String, String> getOverrides() {
		return this.overrides;
	}

	void forEachPrefix(Consumer<String> action) {
		this.prefixes.forEach(action);
	}

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
