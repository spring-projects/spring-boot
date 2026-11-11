/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.devtools.system.DevToolsEnablementDeducer;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A condition that checks if DevTools should be enabled.
 *
 * @author Madhura Bhave
 */
class OnEnabledDevToolsCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("Devtools");
		boolean shouldEnable = DevToolsEnablementDeducer.shouldEnable(Thread.currentThread());
		if (!shouldEnable) {
			return ConditionOutcome.noMatch(message.because("devtools is disabled for current context."));
		}
		return ConditionOutcome.match(message.because("devtools enabled."));
	}

}
