/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure.condition;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

/**
 * {@link Condition} that checks for particular {@link SemanticConventions}.
 *
 * @author Andy Wilkinson
 * @see ConditionalOnSemanticConventions
 */
class OnSemanticConventionsCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, @Nullable Object> attributes = metadata
			.getAnnotationAttributes(ConditionalOnSemanticConventions.class.getName());
		Assert.state(attributes != null, "'attributes' must not be null");
		SemanticConventions semanticConvention = (SemanticConventions) attributes.get("value");
		Assert.state(semanticConvention != null, "'semanticConvention' must not be null");
		return getMatchOutcome(context.getEnvironment(), semanticConvention);
	}

	private ConditionOutcome getMatchOutcome(Environment environment, SemanticConventions semanticConvention) {
		String name = semanticConvention.name();
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnSemanticConventions.class);
		if (semanticConvention.isActive(environment)) {
			return ConditionOutcome.match(message.foundExactly(name));
		}
		return ConditionOutcome.noMatch(message.didNotFind(name).atAll());
	}

}
