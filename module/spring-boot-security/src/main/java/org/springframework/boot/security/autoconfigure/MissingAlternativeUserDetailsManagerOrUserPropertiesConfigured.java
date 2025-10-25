/*
 * Copyright 2012-present the original author or authors.
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
