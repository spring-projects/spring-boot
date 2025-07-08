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
public class OAuth2ClientAutoConfiguration {

	static class NonReactiveWebApplicationCondition extends NoneNestedConditions {

		NonReactiveWebApplicationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = Type.REACTIVE)
		static class ReactiveWebApplicationCondition {

		}

	}

}
