/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Condition;

/**
 * {@link Condition} used to check if security username or password properties have been
 * set or there are no alternatives to the user details manager available.
 *
 * @author Andy Wilkinson
 */
final class MissingAlternativeUserDetailsManagerOrUserPropertiesConfigured extends AnyNestedCondition {

	MissingAlternativeUserDetailsManagerOrUserPropertiesConfigured() {
		super(ConfigurationPhase.PARSE_CONFIGURATION);
	}

	@ConditionalOnMissingClass({ "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository",
			"org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector",
			"org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository" })
	static final class MissingAlternative {

	}

	@ConditionalOnProperty("spring.security.user.name")
	static final class NameConfigured {

	}

	@ConditionalOnProperty("spring.security.user.password")
	static final class PasswordConfigured {

	}

}
