/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OnTracingEnabledCondition}.
 *
 * @author Moritz Halbritter
 */
class OnTracingEnabledConditionTests {

	private final OnTracingEnabledCondition condition = new OnTracingEnabledCondition();

	@Test
	void shouldMatchIfTrue() {
		ConditionOutcome outcome = getMatchOutcome(
				new MockEnvironment().withProperty("management.tracing.enabled", "true"));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).isEqualTo("@ConditionalOnEnabledTracing management.tracing.enabled is true");
	}

	@Test
	void shouldMatchIfMissing() {
		ConditionOutcome outcome = getMatchOutcome(new MockEnvironment());
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).isEqualTo("@ConditionalOnEnabledTracing management.tracing.enabled is true");
	}

	@Test
	void shouldNotMatchIfFalse() {
		ConditionOutcome outcome = getMatchOutcome(
				new MockEnvironment().withProperty("management.tracing.enabled", "false"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).isEqualTo("@ConditionalOnEnabledTracing management.tracing.enabled is false");
	}

	private ConditionOutcome getMatchOutcome(Environment environment) {
		ConditionContext conditionContext = mock(ConditionContext.class);
		given(conditionContext.getEnvironment()).willReturn(environment);
		return this.condition.getMatchOutcome(conditionContext, mock(AnnotatedTypeMetadata.class));
	}

}
