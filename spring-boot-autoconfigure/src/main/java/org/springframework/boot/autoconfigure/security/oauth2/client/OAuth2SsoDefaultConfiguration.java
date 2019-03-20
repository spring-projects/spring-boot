/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2SsoDefaultConfiguration.NeedsWebSecurityCondition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.util.ClassUtils;

/**
 * Configuration for OAuth2 Single Sign On (SSO). If the user only has
 * {@code @EnableOAuth2Sso} but not on a {@code WebSecurityConfigurerAdapter} then one is
 * added with all paths secured and with an order that puts it ahead of the default HTTP
 * Basic security chain in Spring Boot.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@Configuration
@Conditional(NeedsWebSecurityCondition.class)
public class OAuth2SsoDefaultConfiguration extends WebSecurityConfigurerAdapter
		implements Ordered {

	private final ApplicationContext applicationContext;

	private final OAuth2SsoProperties sso;

	public OAuth2SsoDefaultConfiguration(ApplicationContext applicationContext,
			OAuth2SsoProperties sso) {
		this.applicationContext = applicationContext;
		this.sso = sso;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.antMatcher("/**").authorizeRequests().anyRequest().authenticated();
		new SsoSecurityConfigurer(this.applicationContext).configure(http);
	}

	@Override
	public int getOrder() {
		if (this.sso.getFilterOrder() != null) {
			return this.sso.getFilterOrder();
		}
		if (ClassUtils.isPresent(
				"org.springframework.boot.actuate.autoconfigure.ManagementServerProperties",
				null)) {
			// If > BASIC_AUTH_ORDER then the existing rules for the actuator
			// endpoints will take precedence. This value is < BASIC_AUTH_ORDER.
			return SecurityProperties.ACCESS_OVERRIDE_ORDER - 5;
		}
		return SecurityProperties.ACCESS_OVERRIDE_ORDER;
	}

	protected static class NeedsWebSecurityCondition extends EnableOAuth2SsoCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			return ConditionOutcome.inverse(super.getMatchOutcome(context, metadata));
		}

	}

}
