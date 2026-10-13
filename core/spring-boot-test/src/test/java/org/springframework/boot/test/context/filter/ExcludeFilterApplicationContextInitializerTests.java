/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ExcludeFilterApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
class ExcludeFilterApplicationContextInitializerTests {

	@Test
	void testConfigurationIsExcluded() {
		SpringApplication application = new SpringApplication(TestApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		AssertableApplicationContext applicationContext = AssertableApplicationContext.get(application::run);
		assertThat(applicationContext).hasSingleBean(TestApplication.class);
		assertThat(applicationContext).doesNotHaveBean(ExcludedTestConfiguration.class);
	}

	@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class) })
	static class TestApplication {

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ExcludedTestConfiguration {

	}

}
