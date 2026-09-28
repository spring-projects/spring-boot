/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
