/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringApplication} when spring web is not on the classpath.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("spring-web*.jar")
class SpringApplicationNoWebTests {

	@Test
	void detectWebApplicationTypeToNone() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.NONE);
	}

	@Test
	void specificApplicationContextClass() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application
			.setApplicationContextFactory(ApplicationContextFactory.ofContextClass(StaticApplicationContext.class));
		ConfigurableApplicationContext context = application.run();
		assertThat(context).isInstanceOf(StaticApplicationContext.class);
		context.close();
	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleConfig {

	}

}
