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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.RequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for an OAuth2 resource server.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@Configuration
@ConditionalOnMissingBean(AuthorizationServerEndpointsConfiguration.class)
public class ResourceServerTokenServicesConfiguration {

	private static final Log logger = LogFactory
			.getLog(ResourceServerTokenServicesConfiguration.class);

	@Configuration
	protected static class UserInfoRestTemplateConfiguration {

		private static final AuthorizationCodeResourceDetails DEFAULT_RESOURCE_DETAILS = new AuthorizationCodeResourceDetails();

		static {
			DEFAULT_RESOURCE_DETAILS.setClientId("<N/A>");
			DEFAULT_RESOURCE_DETAILS.setUserAuthorizationUri("Not a URI "
					+ "because there is no client");
			DEFAULT_RESOURCE_DETAILS.setAccessTokenUri("Not a URI "
					+ "because there is no client");
		}

		@Autowired(required = false)
		private List<UserInfoRestTemplateCustomizer> customizers = Collections
				.emptyList();

		@Autowired(required = false)
		private OAuth2ProtectedResourceDetails details;

		@Autowired(required = false)
		private OAuth2ClientContext oauth2ClientContext;

		@Bean(name = "userInfoRestTemplate")
		public OAuth2RestTemplate userInfoRestTemplate() {
			if (this.details == null) {
				this.details = DEFAULT_RESOURCE_DETAILS;
			}
			OAuth2RestTemplate template = getTemplate();
			template.setInterceptors(Arrays
					.<ClientHttpRequestInterceptor> asList(new AcceptJsonRequestInterceptor()));
			AuthorizationCodeAccessTokenProvider accessTokenProvider = new AuthorizationCodeAccessTokenProvider();
			accessTokenProvider.setTokenRequestEnhancer(new AcceptJsonRequestEnhancer());
			template.setAccessTokenProvider(accessTokenProvider);
			AnnotationAwareOrderComparator.sort(this.customizers);
			for (UserInfoRestTemplateCustomizer customizer : this.customizers) {
				customizer.customize(template);
			}
			return template;
		}

		private OAuth2RestTemplate getTemplate() {
			if (this.oauth2ClientContext == null) {
				return new OAuth2RestTemplate(this.details);
			}
			return new OAuth2RestTemplate(this.details, this.oauth2ClientContext);
		}

	}

	@Configuration
	@Conditional(NotJwtTokenCondition.class)
	protected static class RemoteTokenServicesConfiguration {

		@Configuration
		@Conditional(TokenInfoCondition.class)
		protected static class TokenInfoServicesConfiguration {

			@Autowired
			private ResourceServerProperties resource;

			@Bean
			public ResourceServerTokenServices remoteTokenServices() {
				RemoteTokenServices services = new RemoteTokenServices();
				services.setCheckTokenEndpointUrl(this.resource.getTokenInfoUri());
				services.setClientId(this.resource.getClientId());
				services.setClientSecret(this.resource.getClientSecret());
				return services;
			}

		}

		@Configuration
		@ConditionalOnClass(OAuth2ConnectionFactory.class)
		@Conditional(NotTokenInfoCondition.class)
		protected static class SocialTokenServicesConfiguration {

			@Autowired
			private ResourceServerProperties sso;

			@Autowired(required = false)
			private OAuth2ConnectionFactory<?> connectionFactory;

			@Autowired(required = false)
			@Qualifier("userInfoRestTemplate")
			private OAuth2RestOperations restTemplate;

			@Bean
			@ConditionalOnBean(ConnectionFactoryLocator.class)
			@ConditionalOnMissingBean(ResourceServerTokenServices.class)
			public SpringSocialTokenServices socialTokenServices() {
				return new SpringSocialTokenServices(this.connectionFactory,
						this.sso.getClientId());
			}

			@Bean
			@ConditionalOnMissingBean({ ConnectionFactoryLocator.class,
					ResourceServerTokenServices.class })
			public ResourceServerTokenServices userInfoTokenServices() {
				UserInfoTokenServices services = new UserInfoTokenServices(
						this.sso.getUserInfoUri(), this.sso.getClientId());
				services.setTokenType(this.sso.getTokenType());
				services.setRestTemplate(this.restTemplate);
				return services;
			}

		}

		@Configuration
		@ConditionalOnMissingClass("org.springframework.social.connect.support.OAuth2ConnectionFactory")
		@Conditional(NotTokenInfoCondition.class)
		protected static class UserInfoTokenServicesConfiguration {

			@Autowired
			private ResourceServerProperties sso;

			@Autowired(required = false)
			@Qualifier("userInfoRestTemplate")
			private OAuth2RestOperations restTemplate;

			@Bean
			@ConditionalOnMissingBean(ResourceServerTokenServices.class)
			public ResourceServerTokenServices userInfoTokenServices() {
				UserInfoTokenServices services = new UserInfoTokenServices(
						this.sso.getUserInfoUri(), this.sso.getClientId());
				services.setRestTemplate(this.restTemplate);
				services.setTokenType(this.sso.getTokenType());
				return services;
			}

		}

	}

	@Configuration
	@Conditional(JwtTokenCondition.class)
	protected static class JwtTokenServicesConfiguration {

		private RestTemplate keyUriRestTemplate = new RestTemplate();

		@Autowired
		private ResourceServerProperties resource;

		@Autowired(required = false)
		private List<JwtAccessTokenConverterConfigurer> configurers = Collections
				.emptyList();

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
					keyValue = getKeyFromServer();
				}
				catch (ResourceAccessException ex) {
					logger.warn("Failed to fetch token key (you may need to refresh "
							+ "when the auth server is back)");
				}
			}
			if (StringUtils.hasText(keyValue) && !keyValue.startsWith("-----BEGIN")) {
				converter.setSigningKey(keyValue);
			}
			if (keyValue != null) {
				converter.setVerifierKey(keyValue);
			}
			AnnotationAwareOrderComparator.sort(this.configurers);
			for (JwtAccessTokenConverterConfigurer configurer : this.configurers) {
				configurer.configure(converter);
			}
			return converter;
		}

		private String getKeyFromServer() {
			HttpHeaders headers = new HttpHeaders();
			String username = this.resource.getClientId();
			String password = this.resource.getClientSecret();
			if (username != null && password != null) {
				byte[] token = Base64.encode((username + ":" + password).getBytes());
				headers.add("Authorization", "Basic " + new String(token));
			}
			HttpEntity<Void> request = new HttpEntity<Void>(headers);
			String url = this.resource.getJwt().getKeyUri();
			return (String) this.keyUriRestTemplate
					.exchange(url, HttpMethod.GET, request, Map.class).getBody()
					.get("value");
		}

	}

	private static class TokenInfoCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment,
					"spring.oauth2.resource.");
			Boolean preferTokenInfo = resolver.getProperty("prefer-token-info",
					Boolean.class);
			if (preferTokenInfo == null) {
				preferTokenInfo = environment.resolvePlaceholders(
						"${OAUTH2_RESOURCE_PREFERTOKENINFO:true}").equals("true");
			}
			String tokenInfoUri = resolver.getProperty("token-info-uri");
			String userInfoUri = resolver.getProperty("user-info-uri");
			if (!StringUtils.hasLength(userInfoUri)) {
				return ConditionOutcome.match("No user info provided");
			}
			if (StringUtils.hasLength(tokenInfoUri) && preferTokenInfo) {
				return ConditionOutcome.match("Token info endpoint "
						+ "is preferred and user info provided");
			}
			return ConditionOutcome.noMatch("Token info endpoint is not provided");
		}

	}

	private static class JwtTokenCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
					context.getEnvironment(), "spring.oauth2.resource.jwt.");
			String keyValue = resolver.getProperty("key-value");
			String keyUri = resolver.getProperty("key-uri");
			if (StringUtils.hasText(keyValue) || StringUtils.hasText(keyUri)) {
				return ConditionOutcome.match("public key is provided");
			}
			return ConditionOutcome.noMatch("public key is not provided");
		}

	}

	private static class NotTokenInfoCondition extends SpringBootCondition {

		private TokenInfoCondition tokenInfoCondition = new TokenInfoCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			return ConditionOutcome.inverse(this.tokenInfoCondition.getMatchOutcome(
					context, metadata));
		}

	}

	private static class NotJwtTokenCondition extends SpringBootCondition {

		private JwtTokenCondition jwtTokenCondition = new JwtTokenCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			return ConditionOutcome.inverse(this.jwtTokenCondition.getMatchOutcome(
					context, metadata));
		}

	}

	private static class AcceptJsonRequestInterceptor implements
			ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			return execution.execute(request, body);
		}

	}

	private static class AcceptJsonRequestEnhancer implements RequestEnhancer {

		@Override
		public void enhance(AccessTokenRequest request,
				OAuth2ProtectedResourceDetails resource,
				MultiValueMap<String, String> form, HttpHeaders headers) {
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		}

	}

}
