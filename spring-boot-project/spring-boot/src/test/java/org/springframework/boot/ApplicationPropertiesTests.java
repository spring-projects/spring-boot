/*
 * Copyright 2012-2025 the original author or authors.
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
		assertThat(RuntimeHintsPredicates.reflection().onMethod(ApplicationProperties.class, "setBannerMode").invoke())
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(ApplicationProperties.class, "getSources").invoke())
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(ApplicationProperties.class, "setSources").invoke())
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(ApplicationProperties.class, "getBannerMode"))
			.rejects(hints);
	}

}
