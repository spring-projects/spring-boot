/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.devtools.autoconfigure.DevToolsDataSourceAutoConfiguration.NonEmbeddedInMemoryDatabaseShutdownExecutor;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DevToolsDataSourceAutoConfiguration} when {@code spring-boot-jdbc} is
 * not on the classpath.
 *
 * @author Kosuke Yanagihara
 */
@ClassPathExclusions(packages = "org.springframework.boot.jdbc.autoconfigure")
class DevToolsDataSourceAutoConfigurationWithoutSpringBootJdbcTests extends AbstractDevToolsAutoConfigurationTests {

	@Test
	void backsOffWithSingleManuallyConfiguredDataSource() throws Exception {
		try (AssertableApplicationContext context = getContext(
				() -> new AnnotationConfigApplicationContext(SingleDataSourceConfiguration.class))) {
			assertThat(context).hasSingleBean(DataSource.class)
				.doesNotHaveBean(NonEmbeddedInMemoryDatabaseShutdownExecutor.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(DevToolsDataSourceAutoConfiguration.class)
	static class SingleDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return mock(DataSource.class);
		}

	}

}
