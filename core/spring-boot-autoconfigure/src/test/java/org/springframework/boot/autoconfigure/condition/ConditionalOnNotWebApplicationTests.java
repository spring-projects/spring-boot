/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConditionalOnNotWebApplication @ConditionalOnNotWebApplication}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnNotWebApplicationTests {

	@Test
	void testNotWebApplicationWithServletContext() {
		new WebApplicationContextRunner().withUserConfiguration(NotWebApplicationConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	void testNotWebApplicationWithReactiveContext() {
		new ReactiveWebApplicationContextRunner().withUserConfiguration(NotWebApplicationConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	void testNotWebApplication() {
		new ApplicationContextRunner().withUserConfiguration(NotWebApplicationConfiguration.class)
			.run((context) -> assertThat(context).getBeans(String.class).containsExactly(entry("none", "none")));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnNotWebApplication
	static class NotWebApplicationConfiguration {

		@Bean
		String none() {
			return "none";
		}

	}

}
