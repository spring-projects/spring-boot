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
 * Tests for {@link ConditionalOnWarDeployment @ConditionalOnWarDeployment}.
 *
 * @author Madhura Bhave
 */
class ConditionalOnWarDeploymentTests {

	@Test
	void nonWebApplicationShouldNotMatch() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void reactiveWebApplicationShouldNotMatch() {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void embeddedServletWebApplicationShouldNotMatch() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebApplicationContext::new);
		contextRunner.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void warDeployedServletWebApplicationShouldMatch() {
		// sets a mock servletContext before context refresh which is what the
		// SpringBootServletInitializer does for WAR deployments.
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).hasBean("forWar"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWarDeployment
	static class TestConfiguration {

		@Bean
		String forWar() {
			return "forWar";
		}

	}

}
