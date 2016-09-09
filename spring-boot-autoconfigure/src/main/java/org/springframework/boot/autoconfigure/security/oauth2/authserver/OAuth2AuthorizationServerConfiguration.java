/*
 * Copyright 2012-2015 the original author or authors.
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

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
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
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.token.TokenStore;

/**
 * Configuration for a Spring Security OAuth2 authorization server. Back off if another
 * {@link AuthorizationServerConfigurer} already exists or if authorization server is not
 * enabled.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(EnableAuthorizationServer.class)
@ConditionalOnMissingBean(AuthorizationServerConfigurer.class)
@ConditionalOnBean(AuthorizationServerEndpointsConfiguration.class)
@EnableConfigurationProperties(AuthorizationServerProperties.class)
public class OAuth2AuthorizationServerConfiguration
		extends AuthorizationServerConfigurerAdapter {

	private static final Log logger = LogFactory
			.getLog(OAuth2AuthorizationServerConfiguration.class);

	@Autowired
	private BaseClientDetails details;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired(required = false)
	private TokenStore tokenStore;

	@Autowired
	private AuthorizationServerProperties properties;

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

		if (this.details.getAutoApproveScopes() != null) {
			builder.autoApprove(
					this.details.getAutoApproveScopes().toArray(new String[0]));
		}
		if (this.details.getAccessTokenValiditySeconds() != null) {
			builder.accessTokenValiditySeconds(
					this.details.getAccessTokenValiditySeconds());
		}
		if (this.details.getRefreshTokenValiditySeconds() != null) {
			builder.refreshTokenValiditySeconds(
					this.details.getRefreshTokenValiditySeconds());
		}
		if (this.details.getRegisteredRedirectUri() != null) {
			builder.redirectUris(
					this.details.getRegisteredRedirectUri().toArray(new String[0]));
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

	@Override
	public void configure(AuthorizationServerSecurityConfigurer security)
			throws Exception {
		if (this.properties.getCheckTokenAccess() != null) {
			security.checkTokenAccess(this.properties.getCheckTokenAccess());
		}
		if (this.properties.getTokenKeyAccess() != null) {
			security.tokenKeyAccess(this.properties.getTokenKeyAccess());
		}
		if (this.properties.getRealm() != null) {
			security.realm(this.properties.getRealm());
		}
	}

	@Configuration
	protected static class ClientDetailsLogger {

		@Autowired
		private OAuth2ClientProperties credentials;

		@PostConstruct
		public void init() {
			String prefix = "security.oauth2.client";
			boolean defaultSecret = this.credentials.isDefaultSecret();
			logger.info(String.format(
					"Initialized OAuth2 Client\n\n%s.clientId = %s\n%s.secret = %s\n\n",
					prefix, this.credentials.getClientId(), prefix,
					defaultSecret ? this.credentials.getClientSecret() : "****"));
		}

	}

	@Configuration
	@ConditionalOnMissingBean(BaseClientDetails.class)
	protected static class BaseClientDetailsConfiguration {

		@Autowired
		private OAuth2ClientProperties client;

		@Bean
		@ConfigurationProperties("security.oauth2.client")
		public BaseClientDetails oauth2ClientDetails() {
			BaseClientDetails details = new BaseClientDetails();
			if (this.client.getClientId() == null) {
				this.client.setClientId(UUID.randomUUID().toString());
			}
			details.setClientId(this.client.getClientId());
			details.setClientSecret(this.client.getClientSecret());
			details.setAuthorizedGrantTypes(Arrays.asList("authorization_code",
					"password", "client_credentials", "implicit", "refresh_token"));
			details.setAuthorities(
					AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
			details.setRegisteredRedirectUri(Collections.<String>emptySet());
			return details;
		}

	}

}
