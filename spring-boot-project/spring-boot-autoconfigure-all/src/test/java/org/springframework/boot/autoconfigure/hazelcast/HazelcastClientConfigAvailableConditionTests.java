/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastClientConfigAvailableCondition}.
 *
 * @author Stephane Nicoll
 */
class HazelcastClientConfigAvailableConditionTests {

	private final HazelcastClientConfigAvailableCondition condition = new HazelcastClientConfigAvailableCondition();

	@Test
	void explicitConfigurationWithClientConfigMatches() {
		ConditionOutcome outcome = getMatchOutcome(new MockEnvironment().withProperty("spring.hazelcast.config",
				"classpath:org/springframework/boot/autoconfigure/hazelcast/hazelcast-client-specific.xml"));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).contains("Hazelcast client configuration detected");
	}

	@Test
	void explicitConfigurationWithServerConfigDoesNotMatch() {
		ConditionOutcome outcome = getMatchOutcome(new MockEnvironment().withProperty("spring.hazelcast.config",
				"classpath:org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).contains("Hazelcast server configuration detected");
	}

	@Test
	void explicitConfigurationWithMissingConfigDoesNotMatch() {
		ConditionOutcome outcome = getMatchOutcome(new MockEnvironment().withProperty("spring.hazelcast.config",
				"classpath:org/springframework/boot/autoconfigure/hazelcast/test-config-does-not-exist.xml"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).contains("Hazelcast configuration does not exist");
	}

	private ConditionOutcome getMatchOutcome(Environment environment) {
		ConditionContext conditionContext = mock(ConditionContext.class);
		given(conditionContext.getEnvironment()).willReturn(environment);
		given(conditionContext.getResourceLoader()).willReturn(new DefaultResourceLoader());
		return this.condition.getMatchOutcome(conditionContext, mock(AnnotatedTypeMetadata.class));
	}

}
