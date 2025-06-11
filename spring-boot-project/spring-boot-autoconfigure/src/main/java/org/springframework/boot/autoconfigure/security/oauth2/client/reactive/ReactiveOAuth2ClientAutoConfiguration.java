/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client.reactive;

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
 * @since 2.1.0
 */
@AutoConfiguration
@Conditional(ReactiveOAuth2ClientAutoConfiguration.NonServletApplicationCondition.class)
@ConditionalOnClass({ Flux.class, ClientRegistration.class })
@Import({ ReactiveOAuth2ClientConfigurations.ReactiveClientRegistrationRepositoryConfiguration.class,
		ReactiveOAuth2ClientConfigurations.ReactiveOAuth2AuthorizedClientServiceConfiguration.class })
public class ReactiveOAuth2ClientAutoConfiguration {

	static class NonServletApplicationCondition extends NoneNestedConditions {

		NonServletApplicationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = Type.SERVLET)
		static class ServletApplicationCondition {

		}

	}

}
