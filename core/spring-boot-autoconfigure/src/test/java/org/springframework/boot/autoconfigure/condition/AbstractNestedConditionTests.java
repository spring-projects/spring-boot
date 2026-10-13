/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractNestedCondition}.
 *
 * @author Razib Shahriar
 */
class AbstractNestedConditionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void validPhase() {
		this.contextRunner.withUserConfiguration(ValidConfig.class)
			.run((context) -> assertThat(context).hasBean("myBean"));
	}

	@Test
	void invalidMemberPhase() {
		this.contextRunner.withUserConfiguration(InvalidConfig.class).run((context) -> {
			assertThat(context).hasFailed();
			Throwable startupFailure = context.getStartupFailure();
			assertThat(startupFailure).isNotNull();
			assertThat(startupFailure.getCause()).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Nested condition " + InvalidNestedCondition.class.getName()
						+ " uses a configuration phase that is inappropriate for class "
						+ OnBeanCondition.class.getName());
		});
	}

	@Test
	void invalidNestedMemberPhase() {
		this.contextRunner.withUserConfiguration(DoubleNestedConfig.class).run((context) -> {
			assertThat(context).hasFailed();
			Throwable startupFailure = context.getStartupFailure();
			assertThat(startupFailure).isNotNull();
			assertThat(startupFailure.getCause()).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Nested condition " + DoubleNestedCondition.class.getName()
						+ " uses a configuration phase that is inappropriate for class "
						+ ValidNestedCondition.class.getName());
		});
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(ValidNestedCondition.class)
	static class ValidConfig {

		@Bean
		String myBean() {
			return "myBean";
		}

	}

	static class ValidNestedCondition extends AbstractNestedCondition {

		ValidNestedCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@Override
		protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
			return ConditionOutcome.match();
		}

		@ConditionalOnMissingBean(name = "myBean")
		static class MissingMyBean {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(InvalidNestedCondition.class)
	static class InvalidConfig {

		@Bean
		String myBean() {
			return "myBean";
		}

	}

	static class InvalidNestedCondition extends AbstractNestedCondition {

		InvalidNestedCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Override
		protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
			return ConditionOutcome.match();
		}

		@ConditionalOnMissingBean(name = "myBean")
		static class MissingMyBean {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(DoubleNestedCondition.class)
	static class DoubleNestedConfig {

	}

	static class DoubleNestedCondition extends AbstractNestedCondition {

		DoubleNestedCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Override
		protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
			return ConditionOutcome.match();
		}

		@Conditional(ValidNestedCondition.class)
		static class NestedConditionThatIsValid {

		}

	}

}
