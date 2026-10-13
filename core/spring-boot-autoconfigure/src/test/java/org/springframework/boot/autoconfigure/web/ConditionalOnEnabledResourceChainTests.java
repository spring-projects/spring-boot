/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnEnabledResourceChain @ConditionalOnEnabledResourceChain}.
 *
 * @author Stephane Nicoll
 */
class ConditionalOnEnabledResourceChainTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@AfterEach
	void closeContext() {
		this.context.close();
	}

	@Test
	void disabledByDefault() {
		load();
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void disabledExplicitly() {
		load("spring.web.resources.chain.enabled:false");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void enabledViaMainEnabledFlag() {
		load("spring.web.resources.chain.enabled:true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void enabledViaFixedStrategyFlag() {
		load("spring.web.resources.chain.strategy.fixed.enabled:true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void enabledViaContentStrategyFlag() {
		load("spring.web.resources.chain.strategy.content.enabled:true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	private void load(String... environment) {
		this.context.register(Config.class);
		TestPropertyValues.of(environment).applyTo(this.context);
		this.context.refresh();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		@ConditionalOnEnabledResourceChain
		String foo() {
			return "foo";
		}

	}

}
