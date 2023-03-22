/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.server.servlet;

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
 * Condition that matches if any {@code spring.security.oauth2.authorizationserver.client}
 * properties are defined.
 *
 * @author Steve Riesenberg
 */
class RegisteredClientsConfiguredCondition extends SpringBootCondition {

	private static final Bindable<Map<String, OAuth2AuthorizationServerProperties.Client>> STRING_CLIENT_MAP = Bindable
		.mapOf(String.class, OAuth2AuthorizationServerProperties.Client.class);

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage
			.forCondition("OAuth2 Registered Clients Configured Condition");
		Map<String, OAuth2AuthorizationServerProperties.Client> registrations = getRegistrations(
				context.getEnvironment());
		if (!registrations.isEmpty()) {
			return ConditionOutcome.match(message.foundExactly("registered clients " + registrations.values()
				.stream()
				.map(OAuth2AuthorizationServerProperties.Client::getRegistration)
				.map(OAuth2AuthorizationServerProperties.Registration::getClientId)
				.collect(Collectors.joining(", "))));
		}
		return ConditionOutcome.noMatch(message.notAvailable("registered clients"));
	}

	private Map<String, OAuth2AuthorizationServerProperties.Client> getRegistrations(Environment environment) {
		return Binder.get(environment)
			.bind("spring.security.oauth2.authorizationserver.client", STRING_CLIENT_MAP)
			.orElse(Collections.emptyMap());
	}

}
