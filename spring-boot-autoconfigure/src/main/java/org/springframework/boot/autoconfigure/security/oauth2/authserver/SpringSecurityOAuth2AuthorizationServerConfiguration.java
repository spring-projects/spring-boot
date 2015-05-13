/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.authserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.ClientCredentialsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.config.annotation.builders.ClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.token.TokenStore;

/**
 * Auto-configure a Spring Security OAuth2 authorization server. Back off if another
 * {@link AuthorizationServerConfigurer} already exists or if authorization server is not
 * enabled.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(EnableAuthorizationServer.class)
@ConditionalOnMissingBean(AuthorizationServerConfigurer.class)
@ConditionalOnBean(AuthorizationServerEndpointsConfiguration.class)
@EnableConfigurationProperties
public class SpringSecurityOAuth2AuthorizationServerConfiguration extends
		AuthorizationServerConfigurerAdapter {

	@Autowired
	private BaseClientDetails details;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired(required = false)
	private TokenStore tokenStore;

	@Configuration
	@ConditionalOnMissingBean(BaseClientDetails.class)
	protected static class BaseClientDetailsConfiguration {

		@Autowired
		private ClientCredentialsProperties client;

		@Bean
		@ConfigurationProperties("spring.oauth2.client")
		public BaseClientDetails oauth2ClientDetails() {
			BaseClientDetails details = new BaseClientDetails();
			if (this.client.getClientId() == null) {
				this.client.setClientId(UUID.randomUUID().toString());
			}
			details.setClientId(this.client.getClientId());
			details.setClientSecret(this.client.getClientSecret());
			details.setAuthorizedGrantTypes(Arrays.asList("authorization_code",
					"password", "client_credentials", "implicit", "refresh_token"));
			details.setAuthorities(AuthorityUtils
					.commaSeparatedStringToAuthorityList("ROLE_USER"));
			details.setRegisteredRedirectUri(Collections.<String> emptySet());
			return details;
		}

	}

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		ClientDetailsServiceBuilder<InMemoryClientDetailsServiceBuilder>.ClientBuilder builder = clients
				.inMemory().withClient(this.details.getClientId());
		builder.secret(this.details.getClientSecret())
				.resourceIds(this.details.getResourceIds().toArray(new String[0]))
				.authorizedGrantTypes(
						this.details.getAuthorizedGrantTypes().toArray(new String[0]))
				.authorities(
						AuthorityUtils.authorityListToSet(this.details.getAuthorities())
								.toArray(new String[0]))
				.scopes(this.details.getScope().toArray(new String[0]));
		if (this.details.getRegisteredRedirectUri() != null) {
			builder.redirectUris(this.details.getRegisteredRedirectUri().toArray(
					new String[0]));
		}
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints)
			throws Exception {
		if (this.tokenStore != null) {
			endpoints.tokenStore(this.tokenStore);
		}
		if (this.details.getAuthorizedGrantTypes().contains("password")) {
			endpoints.authenticationManager(this.authenticationManager);
		}
	}

}
