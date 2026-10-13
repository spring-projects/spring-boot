/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

/**
 * {@link Condition} that checks for a required {@link Threading}.
 *
 * @author Moritz Halbritter
 * @see ConditionalOnThreading
 */
class OnThreadingCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, @Nullable Object> attributes = metadata
			.getAnnotationAttributes(ConditionalOnThreading.class.getName());
		Assert.state(attributes != null, "'attributes' must not be null");
		Threading threading = (Threading) attributes.get("value");
		Assert.state(threading != null, "'threading' must not be null");
		return getMatchOutcome(context.getEnvironment(), threading);
	}

	private ConditionOutcome getMatchOutcome(Environment environment, Threading threading) {
		String name = threading.name();
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnThreading.class);
		if (threading.isActive(environment)) {
			return ConditionOutcome.match(message.foundExactly(name));
		}
		return ConditionOutcome.noMatch(message.didNotFind(name).atAll());
	}

}
