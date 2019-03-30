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

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A Condition that evaluates a SpEL expression.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @see ConditionalOnExpression
 */
@Order(Ordered.LOWEST_PRECEDENCE - 20)
class OnExpressionCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		String expression = (String) metadata
				.getAnnotationAttributes(ConditionalOnExpression.class.getName())
				.get("value");
		expression = wrapIfNecessary(expression);
		ConditionMessage.Builder messageBuilder = ConditionMessage
				.forCondition(ConditionalOnExpression.class, "(" + expression + ")");
		expression = context.getEnvironment().resolvePlaceholders(expression);
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory != null) {
			boolean result = evaluateExpression(beanFactory, expression);
			return new ConditionOutcome(result, messageBuilder.resultedIn(result));
		}
		return ConditionOutcome
				.noMatch(messageBuilder.because("no BeanFactory available."));
	}

	private Boolean evaluateExpression(ConfigurableListableBeanFactory beanFactory,
			String expression) {
		BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
		if (resolver == null) {
			resolver = new StandardBeanExpressionResolver();
		}
		BeanExpressionContext expressionContext = new BeanExpressionContext(beanFactory,
				null);
		Object result = resolver.evaluate(expression, expressionContext);
		return (result != null && (boolean) result);
	}

	/**
	 * Allow user to provide bare expression with no '#{}' wrapper.
	 * @param expression source expression
	 * @return wrapped expression
	 */
	private String wrapIfNecessary(String expression) {
		if (!expression.startsWith("#{")) {
			return "#{" + expression + "}";
		}
		return expression;
	}

}
