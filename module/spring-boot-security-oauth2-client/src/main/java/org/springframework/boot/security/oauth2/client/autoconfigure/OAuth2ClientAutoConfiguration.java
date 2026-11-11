/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration.NonReactiveWebApplicationCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth client support.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
@Conditional(NonReactiveWebApplicationCondition.class)
@ConditionalOnClass(ClientRegistration.class)
@Import({ OAuth2ClientConfigurations.ClientRegistrationRepositoryConfiguration.class,
		OAuth2ClientConfigurations.OAuth2AuthorizedClientServiceConfiguration.class })
public final class OAuth2ClientAutoConfiguration {

	static class NonReactiveWebApplicationCondition extends NoneNestedConditions {

		NonReactiveWebApplicationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = Type.REACTIVE)
		static class ReactiveWebApplicationCondition {

		}

	}

}
