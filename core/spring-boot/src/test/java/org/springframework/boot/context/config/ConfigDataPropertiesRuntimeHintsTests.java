/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.context.config.ConfigDataProperties.Activate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataPropertiesRuntimeHints}.
 *
 * @author Moritz Halbritter
 */
class ConfigDataPropertiesRuntimeHintsTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = new RuntimeHints();
		new ConfigDataPropertiesRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(ConfigDataProperties.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(ConfigDataLocation.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(Activate.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(ConfigDataLocation.class, "of"))
			.accepts(hints);
	}

}
