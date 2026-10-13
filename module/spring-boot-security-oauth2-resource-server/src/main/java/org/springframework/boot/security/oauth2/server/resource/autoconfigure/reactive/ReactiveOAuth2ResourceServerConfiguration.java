/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;

/**
 * Configuration classes for OAuth2 Resource Server These should be {@code @Import} in a
 * regular auto-configuration class to guarantee their order of execution.
 *
 * @author Madhura Bhave
 */
class ReactiveOAuth2ResourceServerConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ BearerTokenAuthenticationToken.class, ReactiveJwtDecoder.class })
	@Import({ ReactiveOAuth2ResourceServerJwtConfiguration.JwtConfiguration.class,
			ReactiveOAuth2ResourceServerJwtConfiguration.JwtConverterConfiguration.class,
			ReactiveOAuth2ResourceServerJwtConfiguration.WebSecurityConfiguration.class })
	static class JwtConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ BearerTokenAuthenticationToken.class, ReactiveOpaqueTokenIntrospector.class })
	@Import({ ReactiveOAuth2ResourceServerOpaqueTokenConfiguration.OpaqueTokenIntrospectionClientConfiguration.class,
			ReactiveOAuth2ResourceServerOpaqueTokenConfiguration.WebSecurityConfiguration.class })
	static class OpaqueTokenConfiguration {

	}

}
