/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnNotWarDeployment @ConditionalOnNotWarDeployment}.
 *
 * @author Guirong Hu
 */
class ConditionalOnNotWarDeploymentTests {

	@Test
	void nonWebApplicationShouldMatch() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner();
		contextRunner.withUserConfiguration(NotWarDeploymentConfiguration.class)
			.run((context) -> assertThat(context).hasBean("notForWar"));
	}

	@Test
	void reactiveWebApplicationShouldMatch() {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner();
		contextRunner.withUserConfiguration(NotWarDeploymentConfiguration.class)
			.run((context) -> assertThat(context).hasBean("notForWar"));
	}

	@Test
	void embeddedServletWebApplicationShouldMatch() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebApplicationContext::new);
		contextRunner.withUserConfiguration(NotWarDeploymentConfiguration.class)
			.run((context) -> assertThat(context).hasBean("notForWar"));
	}

	@Test
	void warDeployedServletWebApplicationShouldNotMatch() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner();
		contextRunner.withUserConfiguration(NotWarDeploymentConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("notForWar"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnNotWarDeployment
	static class NotWarDeploymentConfiguration {

		@Bean
		String notForWar() {
			return "notForWar";
		}

	}

}
