/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure.reactive;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security's Reactive
 * OAuth2 client.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration
@Conditional(ReactiveOAuth2ClientAutoConfiguration.NonServletApplicationCondition.class)
@ConditionalOnClass({ Flux.class, ClientRegistration.class })
@Import({ ReactiveOAuth2ClientConfigurations.ReactiveClientRegistrationRepositoryConfiguration.class,
		ReactiveOAuth2ClientConfigurations.ReactiveOAuth2AuthorizedClientServiceConfiguration.class })
public final class ReactiveOAuth2ClientAutoConfiguration {

	static class NonServletApplicationCondition extends NoneNestedConditions {

		NonServletApplicationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = Type.SERVLET)
		static class ServletApplicationCondition {

		}

	}

}
