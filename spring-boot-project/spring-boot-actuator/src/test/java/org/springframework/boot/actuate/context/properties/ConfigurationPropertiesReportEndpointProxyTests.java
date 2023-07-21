/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.context.properties;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint} when used against a proxy
 * class.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class ConfigurationPropertiesReportEndpointProxyTests {

	@Test
	void testWithProxyClass() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(Config.class,
				SqlExecutor.class);
		contextRunner.run((context) -> {
			ConfigurationPropertiesDescriptor applicationProperties = context
				.getBean(ConfigurationPropertiesReportEndpoint.class)
				.configurationProperties();
			assertThat(applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.values()
				.stream()
				.map(ConfigurationPropertiesBeanDescriptor::getPrefix)
				.filter("executor.sql"::equals)
				.findFirst()).isNotEmpty();
		});
	}

	@Test
	void proxiedConstructorBoundPropertiesShouldBeAvailableInReport() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ValidatedConfiguration.class)
			.withPropertyValues("validated.name=baz");
		contextRunner.run((context) -> {
			ConfigurationPropertiesDescriptor applicationProperties = context
				.getBean(ConfigurationPropertiesReportEndpoint.class)
				.configurationProperties();
			Map<String, Object> properties = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.values()
				.stream()
				.map(ConfigurationPropertiesBeanDescriptor::getProperties)
				.findFirst()
				.get();
			assertThat(properties).containsEntry("name", "baz");
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement(proxyTargetClass = false)
	@EnableConfigurationProperties
	static class Config {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.ALWAYS);
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		static MethodValidationPostProcessor testPostProcessor() {
			return new MethodValidationPostProcessor();
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
		}

	}

	interface Executor {

		void execute();

	}

	abstract static class AbstractExecutor implements Executor {

	}

	@Component
	@ConfigurationProperties("executor.sql")
	static class SqlExecutor extends AbstractExecutor {

		@Override
		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void execute() {
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ValidatedConstructorBindingProperties.class)
	@Import(Config.class)
	static class ValidatedConfiguration {

	}

}
