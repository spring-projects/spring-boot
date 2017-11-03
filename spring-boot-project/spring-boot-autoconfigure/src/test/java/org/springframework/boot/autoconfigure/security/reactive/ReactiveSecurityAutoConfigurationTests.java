/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.reactive;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.MockReactiveWebServerFactory;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class ReactiveSecurityAutoConfigurationTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner();

	@Test
	public void enablesWebFluxSecurity() {
		this.contextRunner.withUserConfiguration(TestConfig.class)
				.withConfiguration(
						AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).getBean(WebFilterChainProxy.class).isNotNull();
					assertThat(context).getBean(WebFluxSecurityConfiguration.class)
							.isNotNull();
					assertThat(context).getBean(WebFilterChainProxy.class).isNotNull();
				});
	}

	@Test
	public void configuresADefaultUser() {
		this.contextRunner.withUserConfiguration(TestConfig.class)
				.withConfiguration(
						AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class))
				.run((context) -> {
					ReactiveUserDetailsService userDetailsService = context
							.getBean(ReactiveUserDetailsService.class);
					assertThat(userDetailsService.findByUsername("user").block())
							.isNotNull();
				});
	}

	@Test
	public void doesNotConfigureDefaultUserIfUserDetailsRepositoryAvailable() {
		this.contextRunner.withUserConfiguration(UserConfig.class, TestConfig.class)
				.withConfiguration(
						AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class))
				.run((context) -> {
					ReactiveUserDetailsService userDetailsService = context
							.getBean(ReactiveUserDetailsService.class);
					assertThat(userDetailsService.findByUsername("user").block())
							.isNull();
					assertThat(userDetailsService.findByUsername("foo").block())
							.isNotNull();
					assertThat(userDetailsService.findByUsername("admin").block())
							.isNotNull();
				});
	}

	@Test
	public void doesNotConfigureDefaultUserIfAuthenticationManagerAvailable() {
		this.contextRunner
				.withUserConfiguration(AuthenticationManagerConfig.class,
						TestConfig.class)
				.withConfiguration(
						AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class))
				.run((context) -> assertThat(context)
						.getBean(ReactiveUserDetailsService.class).isNull());
	}

	@Configuration
	@EnableWebFlux
	static class TestConfig {

		@Bean
		public HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		public ReactiveWebServerFactory reactiveWebServerFactory() {
			return new MockReactiveWebServerFactory();
		}

	}

	@Configuration
	static class UserConfig {

		@Bean
		public MapReactiveUserDetailsService userDetailsService() {
			UserDetails foo = User.withUsername("foo").password("foo").roles("USER")
					.build();
			UserDetails admin = User.withUsername("admin").password("admin")
					.roles("USER", "ADMIN").build();
			return new MapReactiveUserDetailsService(foo, admin);
		}

	}

	@Configuration
	static class AuthenticationManagerConfig {

		@Bean
		public ReactiveAuthenticationManager reactiveAuthenticationManager() {
			return new ReactiveAuthenticationManager() {
				@Override
				public Mono<Authentication> authenticate(Authentication authentication) {
					return null;
				}
			};
		}

	}

}
