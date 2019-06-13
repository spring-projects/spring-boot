/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.reactor.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactorCoreAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class ReactorCoreAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactorCoreAutoConfiguration.class));

	@BeforeEach
	@AfterEach
	void resetDebugFlag() {
		Hooks.resetOnOperatorDebug();
	}

	@Test
	void debugOperatorIsDisabledByDefault() {
		this.contextRunner.run(assertDebugOperator(false));
	}

	@Test
	void debugOperatorIsSetWithProperty() {
		this.contextRunner.withPropertyValues("spring.reactor.debug=true").run(assertDebugOperator(true));
	}

	@Test
	@Deprecated
	void debugOperatorIsSetWithDeprecatedProperty() {
		this.contextRunner.withPropertyValues("spring.reactor.stacktrace-mode.enabled=true")
				.run(assertDebugOperator(true));
	}

	private ContextConsumer<AssertableApplicationContext> assertDebugOperator(boolean expected) {
		return (context) -> assertThat(ReflectionTestUtils.getField(Hooks.class, "GLOBAL_TRACE")).isEqualTo(expected);
	}

}
