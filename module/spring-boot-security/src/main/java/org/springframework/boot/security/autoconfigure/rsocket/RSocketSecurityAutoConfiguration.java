/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.rsocket;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.rsocket.autoconfigure.RSocketMessageHandlerCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.rsocket.core.SecuritySocketAcceptorInterceptor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security for an RSocket
 * server.
 *
 * @author Madhura Bhave
 * @author Brian Clozel
 * @author Guirong Hu
 * @since 4.0.0
 */
@AutoConfiguration
@EnableRSocketSecurity
@ConditionalOnClass({ RSocketServerCustomizer.class, SecuritySocketAcceptorInterceptor.class })
public final class RSocketSecurityAutoConfiguration {

	@Bean
	RSocketServerCustomizer springSecurityRSocketSecurity(SecuritySocketAcceptorInterceptor interceptor) {
		return (server) -> server.interceptors((registry) -> registry.forSocketAcceptor(interceptor));
	}

	@ConditionalOnClass(AuthenticationPrincipalArgumentResolver.class)
	@Configuration(proxyBeanMethods = false)
	static class RSocketSecurityMessageHandlerConfiguration {

		@Bean
		RSocketMessageHandlerCustomizer rSocketAuthenticationPrincipalMessageHandlerCustomizer() {
			return (messageHandler) -> messageHandler.getArgumentResolverConfigurer()
				.addCustomResolver(new AuthenticationPrincipalArgumentResolver());
		}

	}

}
