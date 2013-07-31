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

package org.springframework.boot.autoconfigure.condition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * A Condition that evaluates a SpEL expression.
 * 
 * @author Dave Syer
 * @see ConditionalOnExpression
 */
public class OnExpressionCondition implements Condition {

	private static Log logger = LogFactory.getLog(OnExpressionCondition.class);

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

		String checking = ConditionLogUtils.getPrefix(logger, metadata);

		String value = (String) metadata.getAnnotationAttributes(
				ConditionalOnExpression.class.getName()).get("value");
		if (!value.startsWith("#{")) {
			// For convenience allow user to provide bare expression with no #{} wrapper
			value = "#{" + value + "}";
		}
		if (logger.isDebugEnabled()) {
			StringBuilder builder = new StringBuilder(checking)
					.append("Evaluating expression");
			if (metadata instanceof ClassMetadata) {
				builder.append(" on " + ((ClassMetadata) metadata).getClassName());
			}
			builder.append(": " + value);
			logger.debug(builder.toString());
		}
		// Explicitly allow environment placeholders inside the expression
		value = context.getEnvironment().resolvePlaceholders(value);
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
		BeanExpressionContext expressionContext = (beanFactory != null) ? new BeanExpressionContext(
				beanFactory, null) : null;
		if (resolver == null) {
			resolver = new StandardBeanExpressionResolver();
		}
		boolean result = (Boolean) resolver.evaluate(value, expressionContext);
		if (logger.isDebugEnabled()) {
			logger.debug(checking + "Finished matching and result is matches=" + result);
		}
		return result;
	}

}
