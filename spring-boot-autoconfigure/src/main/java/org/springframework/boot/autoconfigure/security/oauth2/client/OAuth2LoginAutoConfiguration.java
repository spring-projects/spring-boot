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

import java.net.URI;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.oauth2.client.OAuth2ClientPropertiesUtil;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth 2.0 / OpenID Connect 1.0 login support.
 *
 * @author Joe Grandja
 * @since 2.0.0
 * @see HttpSecurity#oauth2Login()
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EnableWebSecurity.class, ClientRegistration.class})
@ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnBean(ClientRegistrationRepository.class)
@AutoConfigureBefore(SecurityAutoConfiguration.class)
@AutoConfigureAfter(ClientRegistrationRepositoryAutoConfiguration.class)
public class OAuth2LoginAutoConfiguration {

	private static final String USER_NAME_ATTR_NAME_PROPERTY = "user-name-attribute-name";

	@Configuration
	protected static class OAuth2LoginConfiguration extends WebSecurityConfigurerAdapter {
		private final ClientRegistrationRepository clientRegistrationRepository;

		private final OAuth2ClientProperties oauth2ClientProperties;

		private final PropertiesPropertySource clientTypesPropertySource;

		protected OAuth2LoginConfiguration(
				ClientRegistrationRepository clientRegistrationRepository,
				OAuth2ClientProperties oauth2ClientProperties) {

			this.clientRegistrationRepository = clientRegistrationRepository;
			this.oauth2ClientProperties = oauth2ClientProperties;
			this.clientTypesPropertySource = OAuth2ClientPropertiesUtil.loadClientTypesPropertySource();
		}

		// @formatter:off
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.oauth2Login()
					.clients(this.clientRegistrationRepository);

			this.registerUserNameAttributeNames(http.oauth2Login());
		}
		// @formatter:on

		private void registerUserNameAttributeNames(OAuth2LoginConfigurer<HttpSecurity> oauth2LoginConfigurer) throws Exception {
			this.oauth2ClientProperties.getRegistrations().forEach((clientKey, clientProperties) -> {
				String userInfoUriValue = clientProperties.getUserInfoUri();
				String userNameAttributeNameValue = (String) this.clientTypesPropertySource.getProperty(
						OAuth2ClientPropertiesUtil.CLIENT_TYPES_PROPERTY_PREFIX + "." +
								clientProperties.getClientType().toString().toLowerCase() + "." +
								USER_NAME_ATTR_NAME_PROPERTY);
				if (userInfoUriValue != null && userNameAttributeNameValue != null) {
					// @formatter:off
					oauth2LoginConfigurer
						.userInfoEndpoint()
							.userNameAttributeName(userNameAttributeNameValue, URI.create(userInfoUriValue));
					// @formatter:on
				}
			});
		}
	}
}
