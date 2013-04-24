/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.context.annotation;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A Condition that evaluates a SpEL expression.
 * 
 * @author Dave Syer
 * 
 */
public class ExpressionCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String value = (String) metadata.getAnnotationAttributes(
				ConditionalOnExpression.class.getName()).get("value");
		if (!value.startsWith("#{")) {
			// For convenience allow user to provide bare expression with no #{} wrapper
			value = "#{" + value + "}";
		}
		// Explicitly allow environment placeholders inside the expression
		value = context.getEnvironment().resolvePlaceholders(value);
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
		BeanExpressionContext expressionContext = (beanFactory != null) ? new BeanExpressionContext(
				beanFactory, null) : null;
		return (Boolean) resolver.evaluate(value, expressionContext);
	}

}