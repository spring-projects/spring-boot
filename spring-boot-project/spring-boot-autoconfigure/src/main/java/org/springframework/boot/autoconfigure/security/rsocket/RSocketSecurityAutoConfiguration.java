/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.rsocket;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessageHandlerCustomizer;
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
 * @since 2.2.0
 */
@AutoConfiguration
@EnableRSocketSecurity
@ConditionalOnClass(SecuritySocketAcceptorInterceptor.class)
public class RSocketSecurityAutoConfiguration {

	/**
	 * Configures the RSocket server to use the provided SecuritySocketAcceptorInterceptor
	 * as an interceptor.
	 * @param interceptor the SecuritySocketAcceptorInterceptor to be used as an
	 * interceptor
	 * @return the RSocketServerCustomizer that configures the RSocket server with the
	 * provided interceptor
	 */
	@Bean
	RSocketServerCustomizer springSecurityRSocketSecurity(SecuritySocketAcceptorInterceptor interceptor) {
		return (server) -> server.interceptors((registry) -> registry.forSocketAcceptor(interceptor));
	}

	/**
	 * RSocketSecurityMessageHandlerConfiguration class.
	 */
	@ConditionalOnClass(AuthenticationPrincipalArgumentResolver.class)
	@Configuration(proxyBeanMethods = false)
	static class RSocketSecurityMessageHandlerConfiguration {

		/**
		 * Customizes the RSocket message handler to include an authentication principal
		 * argument resolver. This resolver is responsible for resolving the
		 * authentication principal from the RSocket message.
		 * @param messageHandler the RSocket message handler to be customized
		 * @return the customized RSocket message handler
		 */
		@Bean
		RSocketMessageHandlerCustomizer rSocketAuthenticationPrincipalMessageHandlerCustomizer() {
			return (messageHandler) -> messageHandler.getArgumentResolverConfigurer()
				.addCustomResolver(new AuthenticationPrincipalArgumentResolver());
		}

	}

}
