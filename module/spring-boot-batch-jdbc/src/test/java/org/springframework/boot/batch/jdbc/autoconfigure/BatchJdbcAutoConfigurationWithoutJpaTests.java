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

package org.springframework.boot.batch.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchJdbcAutoConfiguration.SpringBootBatchJdbcConfiguration;
import org.springframework.boot.batch.jdbc.autoconfigure.domain.City;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BatchJdbcAutoConfiguration} when JPA is not on the classpath.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("hibernate-jpa-*.jar")
class BatchJdbcAutoConfigurationWithoutJpaTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BatchJdbcAutoConfiguration.class, TransactionAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class));

	@Test
	void jdbcWithDefaultSettings() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.datasource.generate-unique-name=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				assertThat(context).hasSingleBean(JobRepository.class);
				assertThat(context.getBean(BatchJdbcProperties.class).getInitializeSchema())
					.isEqualTo(DatabaseInitializationMode.EMBEDDED);
				assertThat(new JdbcTemplate(context.getBean(DataSource.class))
					.queryForList("select * from BATCH_JOB_EXECUTION")).isEmpty();
				assertThat(context.getBean(JobRepository.class).findRunningJobExecutions("test")).isEmpty();
				assertThat(context.getBean(JobRepository.class).getLastJobExecution("test", new JobParameters()))
					.isNull();
			});
	}

	@Test
	@WithPackageResources("custom-schema.sql")
	void jdbcWithCustomPrefix() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.datasource.generate-unique-name=true",
					"spring.batch.jdbc.schema:classpath:custom-schema.sql", "spring.batch.jdbc.tablePrefix:PREFIX_")
			.run((context) -> {
				assertThat(new JdbcTemplate(context.getBean(DataSource.class))
					.queryForList("select * from PREFIX_JOB_EXECUTION")).isEmpty();
				assertThat(context.getBean(JobRepository.class).findRunningJobExecutions("test")).isEmpty();
				assertThat(context.getBean(JobRepository.class).getLastJobExecution("test", new JobParameters()))
					.isNull();
			});
	}

	@Test
	void jdbcWithCustomIsolationLevel() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.datasource.generate-unique-name=true",
					"spring.batch.jdbc.isolation-level-for-create=read_committed")
			.run((context) -> assertThat(
					context.getBean(SpringBootBatchJdbcConfiguration.class).getIsolationLevelForCreate())
				.isEqualTo(Isolation.READ_COMMITTED));
	}

	@TestAutoConfigurationPackage(City.class)
	static class DefaultConfiguration {

	}

}
