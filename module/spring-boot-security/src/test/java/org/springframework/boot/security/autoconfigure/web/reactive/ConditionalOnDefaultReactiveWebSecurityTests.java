/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionalOnDefaultReactiveWebSecurity}.
 *
 * @author Andy Wilkinson
 */
class ConditionalOnDefaultReactiveWebSecurityTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void matchesWithoutSecurityWebFilterChainBean() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TestConfiguration.class))
			.run((context) -> assertThat(context).hasBean("testBean"));
	}

	@Test
	void doesNotMatchWhenSecurityWebFilterChainBeanIsDefined() {
		this.contextRunner.withBean(SecurityWebFilterChain.class, () -> mock(SecurityWebFilterChain.class))
			.withConfiguration(AutoConfigurations.of(TestConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean("testBean"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultReactiveWebSecurity
	static class TestConfiguration {

		@Bean
		String testBean() {
			return "test";
		}

	}

}
