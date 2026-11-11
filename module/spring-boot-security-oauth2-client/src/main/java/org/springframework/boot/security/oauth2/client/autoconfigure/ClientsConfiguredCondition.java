/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches if any {@code spring.security.oauth2.client.registration}
 * properties are defined.
 *
 * @author Madhura Bhave
 */
class ClientsConfiguredCondition extends SpringBootCondition {

	private static final Bindable<Map<String, OAuth2ClientProperties.Registration>> STRING_REGISTRATION_MAP = Bindable
		.mapOf(String.class, OAuth2ClientProperties.Registration.class);

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("OAuth2 Clients Configured Condition");
		Map<String, OAuth2ClientProperties.Registration> registrations = getRegistrations(context.getEnvironment());
		if (!registrations.isEmpty()) {
			return ConditionOutcome.match(message.foundExactly("registered clients " + registrations.values()
				.stream()
				.map(OAuth2ClientProperties.Registration::getClientId)
				.collect(Collectors.joining(", "))));
		}
		return ConditionOutcome.noMatch(message.notAvailable("registered clients"));
	}

	private Map<String, OAuth2ClientProperties.Registration> getRegistrations(Environment environment) {
		return Binder.get(environment)
			.bind("spring.security.oauth2.client.registration", STRING_REGISTRATION_MAP)
			.orElse(Collections.emptyMap());
	}

}
