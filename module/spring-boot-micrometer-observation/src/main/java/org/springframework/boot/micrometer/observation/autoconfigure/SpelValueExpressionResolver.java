/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import io.micrometer.common.annotation.ValueExpressionResolver;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.Assert;

/**
 * A {@link Expression SpEL}-based {@link ValueExpressionResolver}.
 *
 * @author Dominique Villard
 */
class SpelValueExpressionResolver implements ValueExpressionResolver {

	private final ExpressionParser expressionParser = new SpelExpressionParser();

	@Override
	public String resolve(String expression, @Nullable Object parameter) {
		try {
			SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
			Expression expressionToEvaluate = this.expressionParser.parseExpression(expression);
			String value = expressionToEvaluate.getValue(context, parameter, String.class);
			Assert.state(value != null, "'value' must not be null");
			return value;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to evaluate SpEL expression '%s'".formatted(expression), ex);
		}
	}

}
