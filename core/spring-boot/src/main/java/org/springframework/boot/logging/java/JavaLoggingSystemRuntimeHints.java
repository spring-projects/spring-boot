/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.java;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} implementation for {@link JavaLoggingSystem}.
 *
 * @author Moritz Halbritter
 */
class JavaLoggingSystemRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.resources().registerPattern("org/springframework/boot/logging/java/logging.properties");
		hints.resources().registerPattern("org/springframework/boot/logging/java/logging-file.properties");
	}

}
