/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.autoconfigure.init;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OnDatabaseInitializationCondition}.
 *
 * @author Stephane Nicoll
 */
class OnDatabaseInitializationConditionTests {

	@Test
	void getMatchOutcomeWithPropertyNoSetMatches() {
		OnDatabaseInitializationCondition condition = new OnTestDatabaseInitializationCondition("test.init-mode");
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(TestPropertyValues.of("test.another", "noise")),
				mock(AnnotatedTypeMetadata.class));
		assertThat(outcome.isMatch()).isTrue();
	}

	@Test
	void getMatchOutcomeWithPropertySetToAlwaysMatches() {
		OnDatabaseInitializationCondition condition = new OnTestDatabaseInitializationCondition("test.init-mode");
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(TestPropertyValues.of("test.init-mode=always")),
				mock(AnnotatedTypeMetadata.class));
		assertThat(outcome.isMatch()).isTrue();
	}

	@Test
	void getMatchOutcomeWithPropertySetToEmbeddedMatches() {
		OnDatabaseInitializationCondition condition = new OnTestDatabaseInitializationCondition("test.init-mode");
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(TestPropertyValues.of("test.init-mode=embedded")),
				mock(AnnotatedTypeMetadata.class));
		assertThat(outcome.isMatch()).isTrue();
	}

	@Test
	void getMatchOutcomeWithPropertySetToNeverDoesNotMatch() {
		OnDatabaseInitializationCondition condition = new OnTestDatabaseInitializationCondition("test.init-mode");
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(TestPropertyValues.of("test.init-mode=never")), mock(AnnotatedTypeMetadata.class));
		assertThat(outcome.isMatch()).isFalse();
	}

	@Test
	void getMatchOutcomeWithPropertySetToEmptyStringIsIgnored() {
		OnDatabaseInitializationCondition condition = new OnTestDatabaseInitializationCondition("test.init-mode");
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(TestPropertyValues.of("test.init-mode")), mock(AnnotatedTypeMetadata.class));
		assertThat(outcome.isMatch()).isTrue();
	}

	@Test
	void getMatchOutcomeWithMultiplePropertiesUsesFirstSet() {
		OnDatabaseInitializationCondition condition = new OnTestDatabaseInitializationCondition("test.init-mode",
				"test.schema-mode", "test.init-schema-mode");
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(TestPropertyValues.of("test.init-schema-mode=embedded")),
				mock(AnnotatedTypeMetadata.class));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).isEqualTo("TestDatabase Initialization test.init-schema-mode is EMBEDDED");
	}

	@Test
	void getMatchOutcomeHasDedicatedDescription() {
		OnDatabaseInitializationCondition condition = new OnTestDatabaseInitializationCondition("test.init-mode");
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(TestPropertyValues.of("test.init-mode=embedded")),
				mock(AnnotatedTypeMetadata.class));
		assertThat(outcome.getMessage()).isEqualTo("TestDatabase Initialization test.init-mode is EMBEDDED");
	}

	@Test
	void getMatchOutcomeHasWhenPropertyIsNotSetHasDefaultDescription() {
		OnDatabaseInitializationCondition condition = new OnTestDatabaseInitializationCondition("test.init-mode");
		ConditionOutcome outcome = condition.getMatchOutcome(mockConditionContext(TestPropertyValues.empty()),
				mock(AnnotatedTypeMetadata.class));
		assertThat(outcome.getMessage()).isEqualTo("TestDatabase Initialization default value is EMBEDDED");
	}

	private ConditionContext mockConditionContext(TestPropertyValues propertyValues) {
		MockEnvironment environment = new MockEnvironment();
		propertyValues.applyTo(environment);
		ConditionContext conditionContext = mock(ConditionContext.class);
		given(conditionContext.getEnvironment()).willReturn(environment);
		return conditionContext;
	}

	static class OnTestDatabaseInitializationCondition extends OnDatabaseInitializationCondition {

		OnTestDatabaseInitializationCondition(String... properties) {
			super("Test", properties);
		}

	}

}
