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

package org.springframework.boot.autoconfigure.condition;

import org.junit.Test;

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
public class AbstractNestedConditionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void validPhase() {
		this.contextRunner.withUserConfiguration(ValidConfig.class)
				.run((context) -> assertThat(context).hasBean("myBean"));
	}

	@Test
	public void invalidMemberPhase() {
		this.contextRunner.withUserConfiguration(InvalidConfig.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure().getCause())
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Nested condition "
							+ InvalidNestedCondition.class.getName()
							+ " uses a configuration phase that is inappropriate for class "
							+ OnBeanCondition.class.getName());
		});
	}

	@Test
	public void invalidNestedMemberPhase() {
		this.contextRunner.withUserConfiguration(DoubleNestedConfig.class)
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure().getCause())
							.isInstanceOf(IllegalStateException.class)
							.hasMessageContaining("Nested condition "
									+ DoubleNestedCondition.class.getName()
									+ " uses a configuration phase that is inappropriate for class "
									+ ValidNestedCondition.class.getName());
				});
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(ValidNestedCondition.class)
	public static class ValidConfig {

		@Bean
		public String myBean() {
			return "myBean";
		}

	}

	static class ValidNestedCondition extends AbstractNestedCondition {

		ValidNestedCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@Override
		protected ConditionOutcome getFinalMatchOutcome(
				MemberMatchOutcomes memberOutcomes) {
			return ConditionOutcome.match();
		}

		@ConditionalOnMissingBean(name = "myBean")
		static class MissingMyBean {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(InvalidNestedCondition.class)
	public static class InvalidConfig {

		@Bean
		public String myBean() {
			return "myBean";
		}

	}

	static class InvalidNestedCondition extends AbstractNestedCondition {

		InvalidNestedCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Override
		protected ConditionOutcome getFinalMatchOutcome(
				MemberMatchOutcomes memberOutcomes) {
			return ConditionOutcome.match();
		}

		@ConditionalOnMissingBean(name = "myBean")
		static class MissingMyBean {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(DoubleNestedCondition.class)
	public static class DoubleNestedConfig {

	}

	static class DoubleNestedCondition extends AbstractNestedCondition {

		DoubleNestedCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Override
		protected ConditionOutcome getFinalMatchOutcome(
				MemberMatchOutcomes memberOutcomes) {
			return ConditionOutcome.match();
		}

		@Conditional(ValidNestedCondition.class)
		static class NestedConditionThatIsValid {

		}

	}

}
