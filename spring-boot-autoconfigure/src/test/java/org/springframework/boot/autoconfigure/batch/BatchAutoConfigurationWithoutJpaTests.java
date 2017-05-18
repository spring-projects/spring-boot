/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.batch;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.junit.runner.classpath.ClassPathExclusions;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BatchAutoConfiguration} when JPA is not on the classpath.
 *
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("hibernate-jpa-*.jar")
public class BatchAutoConfigurationWithoutJpaTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void jdbcWithDefaultSettings() throws Exception {
		load(new Class<?>[] { DefaultConfiguration.class,
						EmbeddedDataSourceConfiguration.class },
				"spring.datasource.generate-unique-name=true");
		assertThat(this.context.getBeansOfType(JobLauncher.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(JobExplorer.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(JobRepository.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(PlatformTransactionManager.class))
				.hasSize(1);
		assertThat(this.context.getBean(PlatformTransactionManager.class).toString())
				.contains("DataSourceTransactionManager");
		assertThat(
				this.context.getBean(BatchProperties.class).getInitializer().isEnabled())
				.isTrue();
		assertThat(new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from BATCH_JOB_EXECUTION")).isEmpty();
		assertThat(this.context.getBean(JobExplorer.class)
				.findRunningJobExecutions("test")).isEmpty();
		assertThat(this.context.getBean(JobRepository.class)
				.getLastJobExecution("test", new JobParameters())).isNull();
	}

	@Test
	public void jdbcWithCustomPrefix() throws Exception {
		load(new Class<?>[] { DefaultConfiguration.class,
						EmbeddedDataSourceConfiguration.class },
				"spring.datasource.generate-unique-name=true",
				"spring.batch.schema:classpath:batch/custom-schema-hsql.sql",
				"spring.batch.tablePrefix:PREFIX_");
		assertThat(new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from PREFIX_JOB_EXECUTION")).isEmpty();
		assertThat(this.context.getBean(JobExplorer.class)
				.findRunningJobExecutions("test")).isEmpty();
		assertThat(this.context.getBean(JobRepository.class)
				.getLastJobExecution("test", new JobParameters())).isNull();

	}

	private void load(Class<?>[] configs, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		if (configs != null) {
			ctx.register(configs);
		}
		ctx.register(BatchAutoConfiguration.class, TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@EnableBatchProcessing
	@TestAutoConfigurationPackage(City.class)
	protected static class DefaultConfiguration {

	}

}
