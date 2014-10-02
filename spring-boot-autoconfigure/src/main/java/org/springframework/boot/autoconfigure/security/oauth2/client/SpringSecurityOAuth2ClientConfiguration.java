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
package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.oauth2.ClientCredentialsProperties;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.RequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ClientConfiguration;
import org.springframework.util.MultiValueMap;

/**
 * @author Dave Syer
 *
 */
@Configuration
@ConditionalOnClass(EnableOAuth2Client.class)
@ConditionalOnBean(OAuth2ClientConfiguration.class)
public class SpringSecurityOAuth2ClientConfiguration {

	private static final Log logger = LogFactory
			.getLog(SpringSecurityOAuth2ClientConfiguration.class);

	@Configuration
	public static class ClientAuthenticationFilterConfiguration {

		@Resource
		@Qualifier("accessTokenRequest")
		private AccessTokenRequest accessTokenRequest;

		@Autowired
		private ClientCredentialsProperties credentials;

		@PostConstruct
		public void init() {
			String prefix = "spring.oauth2.client";
			boolean defaultSecret = this.credentials.isDefaultSecret();
			logger.info(String.format(
					"Initialized OAuth2 Client\n\n%s.clientId = %s\n%s.secret = %s\n\n",
					prefix, this.credentials.getClientId(), prefix,
					defaultSecret ? this.credentials.getClientSecret() : "****"));
		}

		@Bean
		@ConfigurationProperties("spring.oauth2.client")
		@Primary
		public AuthorizationCodeResourceDetails authorizationCodeResourceDetails() {
			AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
			details.setClientSecret(this.credentials.getClientSecret());
			details.setClientId(this.credentials.getClientId());
			return details;
		}

		@Bean
		public FilterRegistrationBean oauth2ClientFilterRegistration(
				OAuth2ClientContextFilter filter) {
			FilterRegistrationBean registration = new FilterRegistrationBean();
			registration.setFilter(filter);
			registration.setOrder(0);
			return registration;
		}

		@Bean
		public OAuth2RestOperations authorizationCodeRestTemplate(
				AuthorizationCodeResourceDetails oauth2RemoteResource) {
			OAuth2RestTemplate template = new OAuth2RestTemplate(oauth2RemoteResource,
					oauth2ClientContext());
			template.setInterceptors(Arrays
					.<ClientHttpRequestInterceptor> asList(new ClientHttpRequestInterceptor() {
						@Override
						public ClientHttpResponse intercept(HttpRequest request,
								byte[] body, ClientHttpRequestExecution execution)
								throws IOException {
							request.getHeaders().setAccept(
									Arrays.asList(MediaType.APPLICATION_JSON));
							return execution.execute(request, body);
						}
					}));
			AuthorizationCodeAccessTokenProvider accessTokenProvider = new AuthorizationCodeAccessTokenProvider();
			accessTokenProvider.setTokenRequestEnhancer(new RequestEnhancer() {
				@Override
				public void enhance(AccessTokenRequest request,
						OAuth2ProtectedResourceDetails resource,
						MultiValueMap<String, String> form, HttpHeaders headers) {
					headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
				}
			});
			template.setAccessTokenProvider(accessTokenProvider);
			return template;
		}

		@Bean
		@Scope(value = "session", proxyMode = ScopedProxyMode.INTERFACES)
		public OAuth2ClientContext oauth2ClientContext() {
			return new DefaultOAuth2ClientContext(this.accessTokenRequest);
		}

	}

}
