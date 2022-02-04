/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.batch;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BatchAutoConfiguration} when JPA is not on the classpath.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("hibernate-jpa-*.jar")
class BatchAutoConfigurationWithoutJpaTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(BatchAutoConfiguration.class, TransactionAutoConfiguration.class));

	@Test
	void jdbcWithDefaultSettings() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.generate-unique-name=true").run((context) -> {
					assertThat(context).hasSingleBean(JobLauncher.class);
					assertThat(context).hasSingleBean(JobExplorer.class);
					assertThat(context).hasSingleBean(JobRepository.class);
					assertThat(context).hasSingleBean(PlatformTransactionManager.class);
					assertThat(context.getBean(PlatformTransactionManager.class).toString())
							.contains("DataSourceTransactionManager");
					assertThat(context.getBean(BatchProperties.class).getJdbc().getInitializeSchema())
							.isEqualTo(DataSourceInitializationMode.EMBEDDED);
					assertThat(new JdbcTemplate(context.getBean(DataSource.class))
							.queryForList("select * from BATCH_JOB_EXECUTION")).isEmpty();
					assertThat(context.getBean(JobExplorer.class).findRunningJobExecutions("test")).isEmpty();
					assertThat(context.getBean(JobRepository.class).getLastJobExecution("test", new JobParameters()))
							.isNull();
				});
	}

	@Test
	void jdbcWithCustomPrefix() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.batch.jdbc.schema:classpath:batch/custom-schema.sql",
						"spring.batch.jdbc.tablePrefix:PREFIX_")
				.run(assertCustomPrefix());
	}

	@Test
	@Deprecated
	void jdbcWithCustomPrefixWithDeprecatedProperties() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.batch.schema:classpath:batch/custom-schema.sql", "spring.batch.tablePrefix:PREFIX_")
				.run(assertCustomPrefix());
	}

	private ContextConsumer<AssertableApplicationContext> assertCustomPrefix() {
		return (context) -> {
			assertThat(new JdbcTemplate(context.getBean(DataSource.class))
					.queryForList("select * from PREFIX_JOB_EXECUTION")).isEmpty();
			assertThat(context.getBean(JobExplorer.class).findRunningJobExecutions("test")).isEmpty();
			assertThat(context.getBean(JobRepository.class).getLastJobExecution("test", new JobParameters())).isNull();
		};
	}

	@EnableBatchProcessing
	@TestAutoConfigurationPackage(City.class)
	static class DefaultConfiguration {

	}

}
