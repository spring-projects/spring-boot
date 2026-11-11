/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveWebSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class ReactiveWebSecurityAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactiveWebSecurityAutoConfiguration.class));

	@Test
	void backsOffWhenWebFilterChainProxyBeanPresent() {
		this.contextRunner.withUserConfiguration(WebFilterChainProxyConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(WebFilterChainProxy.class));
	}

	@Test
	void autoConfiguresDenyAllReactiveAuthenticationManagerWhenNoAlternativeIsAvailable() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(ReactiveWebSecurityAutoConfiguration.class)
				.hasBean("denyAllAuthenticationManager"));
	}

	@Test
	void enablesWebFluxSecurityWhenUserDetailsServiceIsPresent() {
		this.contextRunner.withUserConfiguration(UserDetailsServiceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(WebFilterChainProxy.class);
			assertThat(context).doesNotHaveBean("denyAllAuthenticationManager");
		});
	}

	@Test
	void enablesWebFluxSecurityWhenReactiveAuthenticationManagerIsPresent() {
		this.contextRunner
			.withBean(ReactiveAuthenticationManager.class, () -> mock(ReactiveAuthenticationManager.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(WebFilterChainProxy.class);
				assertThat(context).doesNotHaveBean("denyAllAuthenticationManager");
			});
	}

	@Test
	void enablesWebFluxSecurityWhenSecurityWebFilterChainIsPresent() {
		this.contextRunner.withBean(SecurityWebFilterChain.class, () -> mock(SecurityWebFilterChain.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(WebFilterChainProxy.class);
				assertThat(context).doesNotHaveBean("denyAllAuthenticationManager");
			});
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
