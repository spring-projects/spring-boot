/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks that a {@link Restarter} is available and initialized.
 *
 * @author Phillip Webb
 * @see ConditionalOnInitializedRestarter
 */
class OnInitializedRestarterCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("Initialized Restarter Condition");
		Restarter restarter = getRestarter();
		if (restarter == null) {
			return ConditionOutcome.noMatch(message.because("unavailable"));
		}
		if (restarter.getInitialUrls() == null) {
			return ConditionOutcome.noMatch(message.because("initialized without URLs"));
		}
		return ConditionOutcome.match(message.because("available and initialized"));
	}

	private @Nullable Restarter getRestarter() {
		try {
			return Restarter.getInstance();
		}
		catch (Exception ex) {
			return null;
		}
	}

}
