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

import io.rsocket.core.RSocketServer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.rsocket.core.SecuritySocketAcceptorInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RSocketSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Brian Clozel
 */
class RSocketSecurityAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RSocketSecurityAutoConfiguration.class,
				RSocketMessagingAutoConfiguration.class, RSocketStrategiesAutoConfiguration.class))
		.withUserConfiguration(UserDetailsServiceConfiguration.class);

	@Test
	void autoConfigurationEnablesRSocketSecurity() {
		this.contextRunner.run((context) -> assertThat(context.getBean(RSocketSecurity.class)).isNotNull());
	}

	@Test
	void autoConfigurationIsConditionalOnSecuritySocketAcceptorInterceptorClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SecuritySocketAcceptorInterceptor.class))
			.run((context) -> assertThat(context).doesNotHaveBean(RSocketSecurity.class));
	}

	@Test
	void autoConfigurationAddsCustomizerForServerRSocketFactory() {
		RSocketServer server = RSocketServer.create();
		this.contextRunner.run((context) -> {
			RSocketServerCustomizer customizer = context.getBean(RSocketServerCustomizer.class);
			customizer.customize(server);
			server.interceptors((registry) -> registry.forSocketAcceptor((interceptors) -> {
				assertThat(interceptors).isNotEmpty();
				assertThat(interceptors)
					.anyMatch((interceptor) -> interceptor instanceof SecuritySocketAcceptorInterceptor);
			}));
		});
	}

	@Test
	void autoConfigurationAddsCustomizerForAuthenticationPrincipalArgumentResolver() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RSocketMessageHandler.class);
			RSocketMessageHandler handler = context.getBean(RSocketMessageHandler.class);
			assertThat(handler.getArgumentResolverConfigurer().getCustomResolvers())
				.anyMatch((customResolver) -> customResolver instanceof AuthenticationPrincipalArgumentResolver);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class UserDetailsServiceConfiguration {

		@Bean
		MapReactiveUserDetailsService userDetailsService() {
			return new MapReactiveUserDetailsService(
					User.withUsername("alice").password("secret").roles("admin").build());
		}

	}

}
