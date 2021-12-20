/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
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
	void whenThereIsNoPlatformTransactionManagerNoTransactionTemplateIsAutoConfigured() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(TransactionTemplate.class));
	}

	@Test
	void whenThereIsASinglePlatformTransactionManagerATransactionTemplateIsAutoConfigured() {
		this.contextRunner.withUserConfiguration(SinglePlatformTransactionManagerConfiguration.class).run((context) -> {
			PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
			TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);
			assertThat(transactionTemplate.getTransactionManager()).isSameAs(transactionManager);
		});
	}

	@Test
	void whenThereIsASingleReactiveTransactionManagerATransactionalOperatorIsAutoConfigured() {
		this.contextRunner.withUserConfiguration(SingleReactiveTransactionManagerConfiguration.class).run((context) -> {
			ReactiveTransactionManager transactionManager = context.getBean(ReactiveTransactionManager.class);
			TransactionalOperator transactionalOperator = context.getBean(TransactionalOperator.class);
			assertThat(transactionalOperator).extracting("transactionManager").isSameAs(transactionManager);
		});
	}

	@Test
	void whenThereAreBothReactiveAndPlatformTransactionManagersATemplateAndAnOperatorAreAutoConfigured() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.withUserConfiguration(SinglePlatformTransactionManagerConfiguration.class,
						SingleReactiveTransactionManagerConfiguration.class)
				.withPropertyValues("spring.datasource.url:jdbc:h2:mem:" + UUID.randomUUID()).run((context) -> {
					PlatformTransactionManager platformTransactionManager = context
							.getBean(PlatformTransactionManager.class);
					TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);
					assertThat(transactionTemplate.getTransactionManager()).isSameAs(platformTransactionManager);
					ReactiveTransactionManager reactiveTransactionManager = context
							.getBean(ReactiveTransactionManager.class);
					TransactionalOperator transactionalOperator = context.getBean(TransactionalOperator.class);
					assertThat(transactionalOperator).extracting("transactionManager")
							.isSameAs(reactiveTransactionManager);
				});
	}

	@Test
	void whenThereAreSeveralPlatformTransactionManagersNoTransactionTemplateIsAutoConfigured() {
		this.contextRunner.withUserConfiguration(SeveralPlatformTransactionManagersConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(TransactionTemplate.class));
	}

	@Test
	void whenThereAreSeveralReactiveTransactionManagersNoTransactionOperatorIsAutoConfigured() {
		this.contextRunner.withUserConfiguration(SeveralReactiveTransactionManagersConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(TransactionalOperator.class));
	}

	@Test
	void whenAUserProvidesATransactionTemplateTheAutoConfiguredTemplateBacksOff() {
		this.contextRunner.withUserConfiguration(CustomPlatformTransactionManagerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(TransactionTemplate.class);
			assertThat(context.getBean("transactionTemplateFoo")).isInstanceOf(TransactionTemplate.class);
		});
	}

	@Test
	void whenAUserProvidesATransactionalOperatorTheAutoConfiguredOperatorBacksOff() {
		this.contextRunner.withUserConfiguration(SingleReactiveTransactionManagerConfiguration.class,
				CustomTransactionalOperatorConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(TransactionalOperator.class);
					assertThat(context.getBean("customTransactionalOperator"))
							.isInstanceOf(TransactionalOperator.class);
				});
	}

	@Test
	void platformTransactionManagerCustomizers() {
		this.contextRunner.withUserConfiguration(SeveralPlatformTransactionManagersConfiguration.class)
				.run((context) -> {
					TransactionManagerCustomizers customizers = context.getBean(TransactionManagerCustomizers.class);
					assertThat(customizers).extracting("customizers").asList().singleElement()
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
		this.contextRunner.withUserConfiguration(PlatformTransactionManagersConfiguration.class).run((context) -> {
			assertThat(context.getBean(AnotherServiceImpl.class).isTransactionActive()).isTrue();
			assertThat(context.getBeansOfType(TransactionalServiceImpl.class)).hasSize(1);
		});
	}

	@Test
	void transactionManagerCanBeConfiguredToJdkProxy() {
		this.contextRunner.withUserConfiguration(PlatformTransactionManagersConfiguration.class)
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
						PlatformTransactionManagersConfiguration.class)
				.withPropertyValues("spring.aop.proxy-target-class=true").run((context) -> {
					assertThat(context.getBean(AnotherService.class).isTransactionActive()).isTrue();
					assertThat(context).doesNotHaveBean(AnotherServiceImpl.class);
					assertThat(context).doesNotHaveBean(TransactionalServiceImpl.class);
				});
	}

	@Configuration
	static class SinglePlatformTransactionManagerConfiguration {

		@Bean
		PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}

	}

	@Configuration
	static class SingleReactiveTransactionManagerConfiguration {

		@Bean
		ReactiveTransactionManager reactiveTransactionManager() {
			return mock(ReactiveTransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SeveralPlatformTransactionManagersConfiguration {

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
	static class SeveralReactiveTransactionManagersConfiguration {

		@Bean
		ReactiveTransactionManager reactiveTransactionManager1() {
			return mock(ReactiveTransactionManager.class);
		}

		@Bean
		ReactiveTransactionManager reactiveTransactionManager2() {
			return mock(ReactiveTransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomPlatformTransactionManagerConfiguration {

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
	static class CustomTransactionalOperatorConfiguration {

		@Bean
		TransactionalOperator customTransactionalOperator() {
			return mock(TransactionalOperator.class);
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
	static class PlatformTransactionManagersConfiguration {

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
