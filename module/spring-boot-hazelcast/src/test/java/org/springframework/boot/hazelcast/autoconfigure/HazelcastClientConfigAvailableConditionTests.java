/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

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
				"classpath:org/springframework/boot/hazelcast/autoconfigure/hazelcast-client-specific.xml"));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).contains("Hazelcast client configuration detected");
	}

	@Test
	void explicitConfigurationWithServerConfigDoesNotMatch() {
		ConditionOutcome outcome = getMatchOutcome(new MockEnvironment().withProperty("spring.hazelcast.config",
				"classpath:org/springframework/boot/hazelcast/autoconfigure/hazelcast-specific.xml"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).contains("Hazelcast server configuration detected");
	}

	@Test
	void explicitConfigurationWithMissingConfigDoesNotMatch() {
		ConditionOutcome outcome = getMatchOutcome(new MockEnvironment().withProperty("spring.hazelcast.config",
				"classpath:org/springframework/boot/hazelcast/autoconfigure/test-config-does-not-exist.xml"));
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
