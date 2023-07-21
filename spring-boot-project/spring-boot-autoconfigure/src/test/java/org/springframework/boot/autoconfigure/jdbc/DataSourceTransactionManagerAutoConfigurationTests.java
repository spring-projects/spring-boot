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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceTransactionManagerAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Davin Byeon
 */
class DataSourceTransactionManagerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TransactionAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class))
		.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:test-" + UUID.randomUUID());

	@Test
	void transactionManagerWithoutDataSourceIsNotConfigured() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(TransactionManager.class));
	}

	@Test
	void transactionManagerWithExistingDataSourceIsConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(TransactionManager.class).hasSingleBean(JdbcTransactionManager.class);
				assertThat(context.getBean(JdbcTransactionManager.class).getDataSource())
					.isSameAs(context.getBean(DataSource.class));
			});
	}

	@Test
	void transactionManagerWithCustomizationIsConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.transaction.default-timeout=1m",
					"spring.transaction.rollback-on-commit-failure=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(TransactionManager.class).hasSingleBean(JdbcTransactionManager.class);
				JdbcTransactionManager transactionManager = context.getBean(JdbcTransactionManager.class);
				assertThat(transactionManager.getDefaultTimeout()).isEqualTo(60);
				assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
			});
	}

	@Test
	void transactionManagerWithExistingTransactionManagerIsNotOverridden() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withBean("myTransactionManager", TransactionManager.class, () -> mock(TransactionManager.class))
			.run((context) -> assertThat(context).hasSingleBean(DataSource.class)
				.hasSingleBean(TransactionManager.class)
				.hasBean("myTransactionManager"));
	}

	@Test // gh-24321
	void transactionManagerWithDaoExceptionTranslationDisabled() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.dao.exceptiontranslation.enabled=false")
			.run((context) -> assertThat(context.getBean(TransactionManager.class))
				.isExactlyInstanceOf(DataSourceTransactionManager.class));
	}

	@Test // gh-24321
	void transactionManagerWithDaoExceptionTranslationEnabled() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.dao.exceptiontranslation.enabled=true")
			.run((context) -> assertThat(context.getBean(TransactionManager.class))
				.isExactlyInstanceOf(JdbcTransactionManager.class));
	}

	@Test // gh-24321
	void transactionManagerWithDaoExceptionTranslationDefault() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> assertThat(context.getBean(TransactionManager.class))
				.isExactlyInstanceOf(JdbcTransactionManager.class));
	}

	@Test
	void transactionWithMultipleDataSourcesIsNotConfigured() {
		this.contextRunner.withUserConfiguration(MultiDataSourceConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(TransactionManager.class));
	}

	@Test
	void transactionWithMultipleDataSourcesAndPrimaryCandidateIsConfigured() {
		this.contextRunner.withUserConfiguration(MultiDataSourceUsingPrimaryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(TransactionManager.class).hasSingleBean(JdbcTransactionManager.class);
			assertThat(context.getBean(JdbcTransactionManager.class).getDataSource())
				.isSameAs(context.getBean("test1DataSource"));
		});
	}

}
