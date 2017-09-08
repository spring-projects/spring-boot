/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2SsoDefaultConfiguration.NeedsWebSecurityCondition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Configuration for OAuth2 Single Sign On (SSO). If the user only has
 * {@code @EnableOAuth2Sso} but not on a {@code WebSecurityConfigurerAdapter} then one is
 * added with all paths secured.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@Configuration
@Conditional(NeedsWebSecurityCondition.class)
public class OAuth2SsoDefaultConfiguration extends WebSecurityConfigurerAdapter {

	private final ApplicationContext applicationContext;

	public OAuth2SsoDefaultConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.antMatcher("/**").authorizeRequests().anyRequest().authenticated();
		new SsoSecurityConfigurer(this.applicationContext).configure(http);
	}

	protected static class NeedsWebSecurityCondition extends EnableOAuth2SsoCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			return ConditionOutcome.inverse(super.getMatchOutcome(context, metadata));
		}

	}

}
