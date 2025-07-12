/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import org.apache.logging.log4j.jul.LogManager;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4J2RuntimeHints}.
 *
 * @author Piotr P. Karwasz
 */
class Log4J2RuntimeHintsTests {

	@Test
	void registersHintsForTypesCheckedByLog4J2LoggingSystem() {
		ReflectionHints reflection = registerHints();
		// Once Log4j Core is reachable, GraalVM will automatically
		// add reachability metadata embedded in the Log4j Core jar and extensions.
		assertThat(reflection.getTypeHint(Log4jContextFactory.class)).isNotNull();
		assertThat(reflection.getTypeHint(Log4jBridgeHandler.class)).isNotNull();
		assertThat(reflection.getTypeHint(LogManager.class)).isNotNull();
	}

	/**
	 *
	 */
	@Test
	void registersHintsForConfigurationFileParsers() {
		ReflectionHints reflection = registerHints();
		// JSON
		assertThat(reflection.getTypeHint(TypeReference.of("com.fasterxml.jackson.databind.ObjectMapper"))).isNotNull();
		// YAML
		assertThat(reflection.getTypeHint(TypeReference.of("com.fasterxml.jackson.dataformat.yaml.YAMLMapper")))
			.isNotNull();
	}

	@Test
	void doesNotRegisterHintsWhenLog4jCoreIsNotAvailable() {
		RuntimeHints hints = new RuntimeHints();
		new Log4J2RuntimeHints().registerHints(hints, ClassLoader.getPlatformClassLoader());
		assertThat(hints.reflection().typeHints()).isEmpty();
	}

	private ReflectionHints registerHints() {
		RuntimeHints hints = new RuntimeHints();
		new Log4J2RuntimeHints().registerHints(hints, getClass().getClassLoader());
		return hints.reflection();
	}

}
