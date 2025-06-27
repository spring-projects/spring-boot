/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.core.aop.MeterTag;
import io.micrometer.tracing.annotation.SpanTag;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

/**
 * Evaluates a Spel expression applied to a parameter for use in {@link MeterTag}
 * {@link SpanTag} Micrometer annotations.
 *
 * @author Dominique Villard
 * @since 4.0.0
 */
public class SpelTagValueExpressionResolver implements ValueExpressionResolver {

	@Override
	public String resolve(String expression, Object parameter) {
		try {
			SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
			ExpressionParser expressionParser = new SpelExpressionParser();
			Expression expressionToEvaluate = expressionParser.parseExpression(expression);
			return expressionToEvaluate.getValue(context, parameter, String.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to evaluate SpEL expression '%s'".formatted(expression), ex);
		}
	}

}
