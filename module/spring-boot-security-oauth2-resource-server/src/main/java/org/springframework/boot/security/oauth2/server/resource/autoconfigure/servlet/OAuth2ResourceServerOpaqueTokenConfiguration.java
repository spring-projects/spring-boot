/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Configures an {@link OpaqueTokenIntrospector} when a token introspection endpoint is
 * available. Also configures a {@link SecurityFilterChain} if a
 * {@link OpaqueTokenIntrospector} bean is found.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
class OAuth2ResourceServerOpaqueTokenConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
	static class OpaqueTokenIntrospectionClientConfiguration {

		@Bean
		@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri")
		SpringOpaqueTokenIntrospector opaqueTokenIntrospector(OAuth2ResourceServerProperties properties) {
			OAuth2ResourceServerProperties.Opaquetoken opaquetoken = properties.getOpaquetoken();
			String introspectionUri = opaquetoken.getIntrospectionUri();
			Assert.state(introspectionUri != null, "'introspectionUri' must not be null");
			String clientId = opaquetoken.getClientId();
			Assert.state(clientId != null, "'clientId' must not be null");
			String clientSecret = opaquetoken.getClientSecret();
			Assert.state(clientSecret != null, "'clientSecret' must not be null");
			return SpringOpaqueTokenIntrospector.withIntrospectionUri(introspectionUri)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultWebSecurity
	static class OAuth2SecurityFilterChainConfiguration {

		@Bean
		@ConditionalOnBean(OpaqueTokenIntrospector.class)
		SecurityFilterChain opaqueTokenSecurityFilterChain(HttpSecurity http) {
			http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
			http.oauth2ResourceServer((resourceServer) -> resourceServer.opaqueToken(withDefaults()));
			return http.build();
		}

	}

}
