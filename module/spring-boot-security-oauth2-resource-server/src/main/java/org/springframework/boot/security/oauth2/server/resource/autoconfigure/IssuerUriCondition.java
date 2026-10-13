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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.StringUtils;

/**
 * Condition for creating {@link JwtDecoder} by oidc issuer location.
 *
 * @author Artsiom Yudovin
 */
class IssuerUriCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("OpenID Connect Issuer URI Condition");
		Environment environment = context.getEnvironment();
		String issuerUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
		if (!StringUtils.hasText(issuerUri)) {
			return ConditionOutcome.noMatch(message.didNotFind("issuer-uri property").atAll());
		}
		String jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
		if (StringUtils.hasText(jwkSetUri)) {
			return ConditionOutcome.noMatch(message.found("jwk-set-uri property").items(jwkSetUri));
		}
		return ConditionOutcome.match(message.foundExactly("issuer-uri property"));
	}

}
