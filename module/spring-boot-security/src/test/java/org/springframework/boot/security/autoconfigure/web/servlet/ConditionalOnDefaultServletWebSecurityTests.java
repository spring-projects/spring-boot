/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionalOnDefaultServletWebSecurity}.
 *
 * @author Andy Wilkinson
 */
class ConditionalOnDefaultServletWebSecurityTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void matchesWithoutSecurityFilterChainBean() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TestConfiguration.class))
			.run((context) -> assertThat(context).hasBean("testBean"));
	}

	@Test
	void doesNotMatchWhenSecurityFilterChainBeanIsDefined() {
		this.contextRunner.withBean(SecurityFilterChain.class, () -> mock(SecurityFilterChain.class))
			.withConfiguration(AutoConfigurations.of(TestConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean("testBean"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultServletWebSecurity
	static class TestConfiguration {

		@Bean
		String testBean() {
			return "test";
		}

	}

}
