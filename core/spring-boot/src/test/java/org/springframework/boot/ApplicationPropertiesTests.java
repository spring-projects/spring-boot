/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.ApplicationProperties.ApplicationPropertiesRuntimeHints;
import org.springframework.boot.Banner.Mode;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationProperties}.
 *
 * @author Moritz Halbritter
 */
class ApplicationPropertiesTests {

	@Test
	void bannerModeShouldBeConsoleIfStructuredLoggingIsNotEnabled() {
		ApplicationProperties properties = new ApplicationProperties();
		assertThat(properties.getBannerMode(new MockEnvironment())).isEqualTo(Mode.CONSOLE);
	}

	@Test
	void bannerModeShouldBeOffIfStructuredLoggingIsEnabled() {
		ApplicationProperties properties = new ApplicationProperties();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.format.console", "ecs");
		assertThat(properties.getBannerMode(environment)).isEqualTo(Mode.OFF);
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = new RuntimeHints();
		new ApplicationPropertiesRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(ApplicationProperties.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(ApplicationProperties.class, "setBannerMode"))
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(ApplicationProperties.class, "getSources"))
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(ApplicationProperties.class, "setSources"))
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(ApplicationProperties.class, "getBannerMode"))
			.rejects(hints);
	}

}
