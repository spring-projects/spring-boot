/*
 * Copyright 2012-2017 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AbstractTransactionManagementConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceTransactionManagerAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 */
public class DataSourceTransactionManagerAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testDataSourceExists() throws Exception {
		this.context.register(EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(DataSourceTransactionManager.class)).isNotNull();
	}

	@Test
	public void testNoDataSourceExists() throws Exception {
		this.context.register(DataSourceTransactionManagerAutoConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(DataSource.class)).isEmpty();
		assertThat(this.context.getBeanNamesForType(DataSourceTransactionManager.class))
				.isEmpty();
	}

	@Test
	public void testManualConfiguration() throws Exception {
		this.context.register(EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(DataSourceTransactionManager.class)).isNotNull();
	}

	@Test
	public void testExistingTransactionManager() {
		this.context.register(TransactionManagerConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(PlatformTransactionManager.class))
				.hasSize(1);
		assertThat(this.context.getBean(PlatformTransactionManager.class))
				.isEqualTo(this.context.getBean("myTransactionManager"));
	}

	@Test
	public void testMultiDataSource() throws Exception {
		this.context.register(MultiDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(PlatformTransactionManager.class))
				.isEmpty();
	}

	@Test
	public void testMultiDataSourceUsingPrimary() throws Exception {
		this.context.register(MultiDataSourceUsingPrimaryConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DataSourceTransactionManager.class)).isNotNull();
		assertThat(this.context.getBean(AbstractTransactionManagementConfiguration.class))
				.isNotNull();
	}

	@Test
	public void testCustomizeDataSourceTransactionManagerUsingProperties()
			throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.transaction.default-timeout:30",
				"spring.transaction.rollback-on-commit-failure:true");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		DataSourceTransactionManager transactionManager = this.context
				.getBean(DataSourceTransactionManager.class);
		assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
		assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
	}

	@Configuration
	protected static class TransactionManagerConfiguration {

		@Bean
		public PlatformTransactionManager myTransactionManager() {
			return mock(PlatformTransactionManager.class);
		}

	}

}
