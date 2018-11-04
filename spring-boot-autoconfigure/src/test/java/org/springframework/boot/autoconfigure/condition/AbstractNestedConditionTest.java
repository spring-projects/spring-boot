/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

/**
 * Tests for {@link AbstractNestedCondition}.
 *
 * @author Razib Shahriar
 */
public class AbstractNestedConditionTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void validMemberPhaseEvaluatesCorrectly() {
		AnnotationConfigApplicationContext context = load(ValidConfig.class);
		assertThat(context.containsBean("myBean"), equalTo(true));
		context.close();
	}

	@Test
	public void invalidMemberPhaseThrowsIllegalState() {
		this.thrown.expectCause(isA(IllegalStateException.class));
		this.thrown.expectCause(hasMessage(
				equalTo("Nested condition " + InvalidNestedCondition.class.getName()
						+ " uses a configuration phase that is inappropriate for class "
						+ OnBeanCondition.class.getName())));
		AnnotationConfigApplicationContext context = load(InvalidConfig.class);
	}

	@Test
	public void invalidNestedMemberPhaseThrowsIllegalState() {
		this.thrown.expectCause(isA(IllegalStateException.class));
		this.thrown.expectCause(hasMessage(
				equalTo("Nested condition " + DoubleNestedCondition.class.getName()
						+ " uses a configuration phase that is inappropriate for class "
						+ ValidNestedCondition.class.getName())));
		AnnotationConfigApplicationContext context = load(DoubleNestedConfig.class);
	}

	private AnnotationConfigApplicationContext load(Class<?> config, String... env) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, env);
		context.register(config);
		context.refresh();
		return context;
	}

	@Configuration
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

	@Configuration
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

	@Configuration
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
