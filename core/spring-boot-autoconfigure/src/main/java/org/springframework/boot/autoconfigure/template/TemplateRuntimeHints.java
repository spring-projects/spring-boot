/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.template;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} for default template location.
 *
 * @author Stephane Nicoll
 */
class TemplateRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.resources().registerPatternIfPresent(classLoader, "templates", (hint) -> hint.includes("templates/**"));
	}

}
