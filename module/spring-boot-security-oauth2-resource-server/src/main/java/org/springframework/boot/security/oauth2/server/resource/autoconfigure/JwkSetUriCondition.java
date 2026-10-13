/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Condition for creating a JWT decoder using a JWK Set URI.
 *
 * @author Vinod Kumar M
 */
class JwkSetUriCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("JWK Set URI Condition");
		Environment environment = context.getEnvironment();
		String jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
		if (!StringUtils.hasText(jwkSetUri)) {
			return ConditionOutcome.noMatch(message.didNotFind("jwk-set-uri property").atAll());
		}
		return ConditionOutcome.match(message.foundExactly("jwk-set-uri property"));
	}

}
