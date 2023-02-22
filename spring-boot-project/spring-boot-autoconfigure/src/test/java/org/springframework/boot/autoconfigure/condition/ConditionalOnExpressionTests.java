/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionalOnExpression @ConditionalOnExpression}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnExpressionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void expressionIsTrue() {
		this.contextRunner.withUserConfiguration(BasicConfiguration.class)
			.run((context) -> assertThat(context.getBean("foo")).isEqualTo("foo"));
	}

	@Test
	void expressionEvaluatesToTrueRegistersBean() {
		this.contextRunner.withUserConfiguration(MissingConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	void expressionEvaluatesToFalseDoesNotRegisterBean() {
		this.contextRunner.withUserConfiguration(NullConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	void expressionEvaluationWithNoBeanFactoryDoesNotMatch() {
		OnExpressionCondition condition = new OnExpressionCondition();
		MockEnvironment environment = new MockEnvironment();
		ConditionContext conditionContext = mock(ConditionContext.class);
		given(conditionContext.getEnvironment()).willReturn(environment);
		ConditionOutcome outcome = condition.getMatchOutcome(conditionContext, mockMetaData("invalid-spel"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).contains("invalid-spel").contains("no BeanFactory available");
	}

	private AnnotatedTypeMetadata mockMetaData(String value) {
		AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
		given(metadata.getAnnotationAttributes(ConditionalOnExpression.class.getName()))
			.willReturn(Collections.singletonMap("value", value));
		return metadata;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnExpression("false")
	static class MissingConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnExpression("true")
	static class BasicConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnExpression("true ? null : false")
	static class NullConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

}
