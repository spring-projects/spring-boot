/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionalOnExpression}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class ConditionalOnExpressionTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void expressionEvaluatesToTrueRegisterBean() {
		this.context.register(BasicConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void expressionEvaluatesToFalseDoesNotRegisterBean() {
		this.context.register(MissingConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void expressionEvaluationWithNoBeanFactoryDoesNotMatch() {
		OnExpressionCondition condition = new OnExpressionCondition();
		MockEnvironment environment = new MockEnvironment();
		ConditionContext conditionContext = mock(ConditionContext.class);
		given(conditionContext.getEnvironment()).willReturn(environment);
		ConditionOutcome outcome = condition.getMatchOutcome(conditionContext,
				mockMetaData("invalid-spel"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).contains("invalid-spel")
				.contains("no BeanFactory available");
	}

	private AnnotatedTypeMetadata mockMetaData(String value) {
		AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("value", value);
		given(metadata.getAnnotationAttributes(ConditionalOnExpression.class.getName()))
				.willReturn(attributes);
		return metadata;
	}

	@Configuration
	@ConditionalOnExpression("false")
	protected static class MissingConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnExpression("true")
	protected static class BasicConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

}
