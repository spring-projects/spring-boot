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

package org.springframework.boot.autoconfigure.security.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.EnableWebFluxSecurityConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class ReactiveSecurityAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class));

	@Test
	void backsOffWhenWebFilterChainProxyBeanPresent() {
		this.contextRunner.withUserConfiguration(WebFilterChainProxyConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(WebFilterChainProxy.class));
	}

	@Test
	void backsOffWhenReactiveAuthenticationManagerNotPresent() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ReactiveSecurityAutoConfiguration.class)
			.doesNotHaveBean(EnableWebFluxSecurityConfiguration.class));
	}

	@Test
	void enablesWebFluxSecurity() {
		this.contextRunner.withUserConfiguration(UserDetailsServiceConfiguration.class)
			.run((context) -> assertThat(context).getBean(WebFilterChainProxy.class).isNotNull());
	}

	@Test
	void autoConfigurationIsConditionalOnClass() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(Flux.class, EnableWebFluxSecurity.class, WebFilterChainProxy.class,
					WebFluxConfigurer.class))
			.withUserConfiguration(UserDetailsServiceConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(WebFilterChainProxy.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class WebFilterChainProxyConfiguration {

		@Bean
		WebFilterChainProxy webFilterChainProxy() {
			return mock(WebFilterChainProxy.class);
		}

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
