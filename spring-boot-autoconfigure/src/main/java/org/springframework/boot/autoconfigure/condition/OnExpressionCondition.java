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
		String rawExpression = expression;
		expression = context.getEnvironment().resolvePlaceholders(expression);
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		BeanExpressionResolver resolver = (beanFactory != null)
				? beanFactory.getBeanExpressionResolver() : null;
		BeanExpressionContext expressionContext = (beanFactory != null)
				? new BeanExpressionContext(beanFactory, null) : null;
		if (resolver == null) {
			resolver = new StandardBeanExpressionResolver();
		}
		Object result = resolver.evaluate(expression, expressionContext);
		boolean match = result != null && (boolean) result;
		return new ConditionOutcome(match, ConditionMessage
				.forCondition(ConditionalOnExpression.class, "(" + rawExpression + ")")
				.resultedIn(result));
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
