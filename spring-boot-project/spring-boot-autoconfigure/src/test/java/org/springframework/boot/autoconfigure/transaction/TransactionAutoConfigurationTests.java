/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.transaction;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class TransactionAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TransactionAutoConfiguration.class));

	@Test
	void noTransactionManager() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(TransactionTemplate.class));
	}

	@Test
	void singleTransactionManager() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.withPropertyValues("spring.datasource.initialization-mode:never").run((context) -> {
					PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
					TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);
					assertThat(transactionTemplate.getTransactionManager()).isSameAs(transactionManager);
				});
	}

	@Test
	void severalTransactionManagers() {
		this.contextRunner.withUserConfiguration(SeveralTransactionManagersConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(TransactionTemplate.class));
	}

	@Test
	void customTransactionManager() {
		this.contextRunner.withUserConfiguration(CustomTransactionManagerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(TransactionTemplate.class);
			assertThat(context.getBean("transactionTemplateFoo")).isInstanceOf(TransactionTemplate.class);
		});
	}

	@Test
	void platformTransactionManagerCustomizers() {
		this.contextRunner.withUserConfiguration(SeveralTransactionManagersConfiguration.class).run((context) -> {
			TransactionManagerCustomizers customizers = context.getBean(TransactionManagerCustomizers.class);
			assertThat(customizers).extracting("customizers").asList().hasSize(1).first()
					.isInstanceOf(TransactionProperties.class);
		});
	}

	@Test
	void transactionNotManagedWithNoTransactionManager() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run(
				(context) -> assertThat(context.getBean(TransactionalService.class).isTransactionActive()).isFalse());
	}

	@Test
	void transactionManagerUsesCglibByDefault() {
		this.contextRunner.withUserConfiguration(TransactionManagersConfiguration.class).run((context) -> {
			assertThat(context.getBean(AnotherServiceImpl.class).isTransactionActive()).isTrue();
			assertThat(context.getBeansOfType(TransactionalServiceImpl.class)).hasSize(1);
		});
	}

	@Test
	void transactionManagerCanBeConfiguredToJdkProxy() {
		this.contextRunner.withUserConfiguration(TransactionManagersConfiguration.class)
				.withPropertyValues("spring.aop.proxy-target-class=false").run((context) -> {
					assertThat(context.getBean(AnotherService.class).isTransactionActive()).isTrue();
					assertThat(context).doesNotHaveBean(AnotherServiceImpl.class);
					assertThat(context).doesNotHaveBean(TransactionalServiceImpl.class);
				});
	}

	@Test
	void customEnableTransactionManagementTakesPrecedence() {
		this.contextRunner
				.withUserConfiguration(CustomTransactionManagementConfiguration.class,
						TransactionManagersConfiguration.class)
				.withPropertyValues("spring.aop.proxy-target-class=true").run((context) -> {
					assertThat(context.getBean(AnotherService.class).isTransactionActive()).isTrue();
					assertThat(context).doesNotHaveBean(AnotherServiceImpl.class);
					assertThat(context).doesNotHaveBean(TransactionalServiceImpl.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class SeveralTransactionManagersConfiguration {

		@Bean
		PlatformTransactionManager transactionManagerOne() {
			return mock(PlatformTransactionManager.class);
		}

		@Bean
		PlatformTransactionManager transactionManagerTwo() {
			return mock(PlatformTransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTransactionManagerConfiguration {

		@Bean
		TransactionTemplate transactionTemplateFoo(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

		@Bean
		PlatformTransactionManager transactionManagerFoo() {
			return mock(PlatformTransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		TransactionalService transactionalService() {
			return new TransactionalServiceImpl();
		}

		@Bean
		AnotherServiceImpl anotherService() {
			return new AnotherServiceImpl();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class TransactionManagersConfiguration {

		@Bean
		DataSourceTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		DataSource dataSource() {
			return DataSourceBuilder.create().driverClassName("org.hsqldb.jdbc.JDBCDriver").url("jdbc:hsqldb:mem:tx")
					.username("sa").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement(proxyTargetClass = false)
	static class CustomTransactionManagementConfiguration {

	}

	interface TransactionalService {

		@Transactional
		boolean isTransactionActive();

	}

	static class TransactionalServiceImpl implements TransactionalService {

		@Override
		public boolean isTransactionActive() {
			return TransactionSynchronizationManager.isActualTransactionActive();
		}

	}

	interface AnotherService {

		boolean isTransactionActive();

	}

	static class AnotherServiceImpl implements AnotherService {

		@Override
		@Transactional
		public boolean isTransactionActive() {
			return TransactionSynchronizationManager.isActualTransactionActive();
		}

	}

}
