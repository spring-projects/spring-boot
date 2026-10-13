/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks that {@link ClientHttpConnectorBuilder} can be detected.
 *
 * @author Phillip Webb
 */
class ConditionalOnClientHttpConnectorBuilderDetection extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		try {
			ClientHttpConnectorBuilder.detect(context.getClassLoader());
			return ConditionOutcome.match("Detected ClientHttpConnectorBuilder");
		}
		catch (IllegalStateException ex) {
			return ConditionOutcome.noMatch("Unable to detect ClientHttpConnectorBuilder");
		}
	}

}
