/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a user-defined {@link ForwardedHeaderStrategy}.
 *
 * @author Phillip Webb
 */
class OnForwardedHeaderStrategyCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ForwardedHeaderStrategy required = metadata.getAnnotations()
			.get(ConditionalOnForwardedHeaderStrategy.class)
			.getEnum(MergedAnnotation.VALUE, ForwardedHeaderStrategy.class);
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnForwardedHeaderStrategy.class);
		if (required == ForwardedHeaderStrategy.get(context.getEnvironment())) {
			return ConditionOutcome.match(message.found("forwarded header strategy").items(required));
		}
		return ConditionOutcome.noMatch(message.didNotFind("forwarded header strategy").items(required));
	}

}
