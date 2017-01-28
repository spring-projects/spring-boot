/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.when;

/**
 * Tests for the {@link OnExpressionCondition}.
 *
 * @author Kristine Jetzke
 */
@RunWith(MockitoJUnitRunner.class)
public class OnExpressionConditionTest {

	@Mock
	private ConditionContext context;

	@Mock
	private AnnotatedTypeMetadata metadata;

	@Mock
	private Environment environment;

	@Mock
	private ConfigurableListableBeanFactory beanFactory;

	@Mock
	private BeanExpressionResolver resolver;

	@Before
	public void setup() {
		when(this.context.getEnvironment()).thenReturn(this.environment);
		when(this.environment.resolvePlaceholders(any())).thenReturn("foo");
		when(this.context.getBeanFactory()).thenReturn(this.beanFactory);
		when(this.beanFactory.getBeanExpressionResolver()).thenReturn(this.resolver);

		when(this.metadata.getAnnotationAttributes(any()))
				.thenReturn(Collections.singletonMap("value", "bar"));
	}

	@Test
	public void nullShouldEvaluateToFalse() {
		when(this.resolver.evaluate(any(), any())).thenReturn(null);

		OnExpressionCondition condition = new OnExpressionCondition();
		ConditionOutcome conditionOutcome = condition.getMatchOutcome(this.context,
				this.metadata);

		assertThat(conditionOutcome.isMatch()).isFalse();
	}

}
