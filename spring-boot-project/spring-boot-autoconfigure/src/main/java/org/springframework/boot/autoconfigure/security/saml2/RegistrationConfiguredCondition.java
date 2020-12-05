/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.saml2;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Registration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches if any {@code spring.security.saml2.relyingparty.registration}
 * properties are defined.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class RegistrationConfiguredCondition extends SpringBootCondition {

	private static final String PROPERTY = "spring.security.saml2.relyingparty.registration";

	private static final Bindable<Map<String, Registration>> STRING_REGISTRATION_MAP = Bindable.mapOf(String.class,
			Registration.class);

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("Relying Party Registration Condition");
		Map<String, Registration> registrations = getRegistrations(context.getEnvironment());
		if (registrations.isEmpty()) {
			return ConditionOutcome.noMatch(message.didNotFind("any registrations").atAll());
		}
		return ConditionOutcome.match(message.found("registration", "registrations").items(registrations.keySet()));
	}

	private Map<String, Registration> getRegistrations(Environment environment) {
		return Binder.get(environment).bind(PROPERTY, STRING_REGISTRATION_MAP).orElse(Collections.emptyMap());
	}

}
