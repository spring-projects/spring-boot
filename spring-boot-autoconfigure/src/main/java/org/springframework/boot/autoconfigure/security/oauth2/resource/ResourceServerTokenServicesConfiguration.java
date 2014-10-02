/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.oauth2.ClientCredentialsProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.SpringSecurityOAuth2ClientConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.SpringSecurityOAuth2ClientConfiguration.ClientAuthenticationFilterConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Dave Syer
 *
 */
@Configuration
@ConditionalOnMissingBean(AuthorizationServerEndpointsConfiguration.class)
public class ResourceServerTokenServicesConfiguration {

	private static final Log logger = LogFactory
			.getLog(ResourceServerTokenServicesConfiguration.class);

	@Configuration
	@Conditional(NotJwtToken.class)
	@EnableOAuth2Client
	@Import(ClientAuthenticationFilterConfiguration.class)
	protected static class RemoteTokenServicesConfiguration {

		@Configuration
		@Import(SpringSecurityOAuth2ClientConfiguration.class)
		@Conditional(TokenInfo.class)
		protected static class TokenInfoServicesConfiguration {

			@Autowired
			private ResourceServerProperties resource;

			@Autowired
			private AuthorizationCodeResourceDetails client;

			@Bean
			public ResourceServerTokenServices remoteTokenServices() {
				RemoteTokenServices services = new RemoteTokenServices();
				services.setCheckTokenEndpointUrl(this.resource.getTokenInfoUri());
				services.setClientId(this.client.getClientId());
				services.setClientSecret(this.client.getClientSecret());
				return services;
			}

		}

		@Configuration
		@ConditionalOnClass(OAuth2ConnectionFactory.class)
		@Conditional(NotTokenInfo.class)
		protected static class SocialTokenServicesConfiguration {

			@Autowired
			private ResourceServerProperties sso;

			@Autowired
			private ClientCredentialsProperties client;

			@Autowired(required = false)
			private OAuth2ConnectionFactory<?> connectionFactory;

			@Autowired(required = false)
			private Map<String, OAuth2RestOperations> resources = Collections.emptyMap();

			@Bean
			@ConditionalOnBean(ConnectionFactoryLocator.class)
			@ConditionalOnMissingBean(ResourceServerTokenServices.class)
			public SpringSocialTokenServices socialTokenServices() {
				return new SpringSocialTokenServices(this.connectionFactory,
						this.client.getClientId());
			}

			@Bean
			@ConditionalOnMissingBean({ ConnectionFactoryLocator.class,
					ResourceServerTokenServices.class })
			public ResourceServerTokenServices userInfoTokenServices() {
				UserInfoTokenServices services = new UserInfoTokenServices(
						this.sso.getUserInfoUri(), this.client.getClientId());
				services.setResources(this.resources);
				return services;
			}

		}

		@Configuration
		@ConditionalOnMissingClass(name = "org.springframework.social.connect.support.OAuth2ConnectionFactory")
		@Conditional(NotTokenInfo.class)
		protected static class UserInfoTokenServicesConfiguration {

			@Autowired
			private ResourceServerProperties sso;

			@Autowired
			private ClientCredentialsProperties client;

			@Autowired(required = false)
			private Map<String, OAuth2RestOperations> resources = Collections.emptyMap();

			@Bean
			@ConditionalOnMissingBean(ResourceServerTokenServices.class)
			public ResourceServerTokenServices userInfoTokenServices() {
				UserInfoTokenServices services = new UserInfoTokenServices(
						this.sso.getUserInfoUri(), this.client.getClientId());
				services.setResources(this.resources);
				return services;
			}

		}

	}

	@Configuration
	@Conditional(JwtToken.class)
	protected static class JwtTokenServicesConfiguration {

		@Autowired
		private ResourceServerProperties resource;

		@Bean
		@ConditionalOnMissingBean(ResourceServerTokenServices.class)
		public ResourceServerTokenServices jwtTokenServices() {
			DefaultTokenServices services = new DefaultTokenServices();
			services.setTokenStore(jwtTokenStore());
			return services;
		}

		@Bean
		public TokenStore jwtTokenStore() {
			return new JwtTokenStore(jwtTokenEnhancer());
		}

		@Bean
		public JwtAccessTokenConverter jwtTokenEnhancer() {
			JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
			String keyValue = this.resource.getJwt().getKeyValue();
			if (!StringUtils.hasText(keyValue)) {
				try {
					keyValue = (String) new RestTemplate().getForObject(
							this.resource.getJwt().getKeyUri(), Map.class).get("value");
				}
				catch (ResourceAccessException e) {
					// ignore
					logger.warn("Failed to fetch token key (you may need to refresh when the auth server is back)");
				}
			}
			else {
				if (StringUtils.hasText(keyValue) && !keyValue.startsWith("-----BEGIN")) {
					converter.setSigningKey(keyValue);
				}
			}
			if (keyValue != null) {
				converter.setVerifierKey(keyValue);
			}
			return converter;
		}

	}

	private static class TokenInfo extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			boolean preferTokenInfo = environment
					.resolvePlaceholders(
							"${spring.oauth2.resource.preferTokenInfo:${OAUTH2_RESOURCE_PREFERTOKENINFO:true}}")
					.equals("true");
			boolean hasTokenInfo = !environment.resolvePlaceholders(
					"${spring.oauth2.resource.tokenInfoUri:}").equals("");
			boolean hasUserInfo = !environment.resolvePlaceholders(
					"${spring.oauth2.resource.userInfoUri:}").equals("");
			if (!hasUserInfo) {
				return ConditionOutcome.match("No user info provided");
			}
			if (hasTokenInfo) {
				if (preferTokenInfo) {
					return ConditionOutcome
							.match("Token info endpoint is preferred and user info provided");
				}
			}
			return ConditionOutcome.noMatch("Token info endpoint is not provided");
		}

	}

	private static class JwtToken extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (StringUtils.hasText(context.getEnvironment().getProperty(
					"spring.oauth2.resource.jwt.keyValue"))
					|| StringUtils.hasText(context.getEnvironment().getProperty(
							"spring.oauth2.resource.jwt.keyUri"))) {
				return ConditionOutcome.match("public key is provided");
			}
			return ConditionOutcome.noMatch("public key is not provided");
		}

	}

	private static class NotTokenInfo extends SpringBootCondition {

		private TokenInfo opposite = new TokenInfo();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionOutcome outcome = this.opposite.getMatchOutcome(context, metadata);
			if (outcome.isMatch()) {
				return ConditionOutcome.noMatch(outcome.getMessage());
			}
			return ConditionOutcome.match(outcome.getMessage());
		}

	}

	private static class NotJwtToken extends SpringBootCondition {

		private JwtToken opposite = new JwtToken();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionOutcome outcome = this.opposite.getMatchOutcome(context, metadata);
			if (outcome.isMatch()) {
				return ConditionOutcome.noMatch(outcome.getMessage());
			}
			return ConditionOutcome.match(outcome.getMessage());
		}

	}
}
