/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.autoconfigure.init;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} for SQL initialization scripts.
 *
 * @author Moritz Halbritter
 */
class SqlInitializationScriptsRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.resources().registerPattern("schema.sql").registerPattern("schema-*.sql");
		hints.resources().registerPattern("data.sql").registerPattern("data-*.sql");
	}

}
