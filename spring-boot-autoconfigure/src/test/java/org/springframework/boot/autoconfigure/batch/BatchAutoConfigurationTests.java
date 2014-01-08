/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringBootTestUtils;
import org.springframework.boot.autoconfigure.ComponentScanDetectorConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link BatchAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class BatchAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultContext() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, BatchAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(JobLauncher.class));
		assertNotNull(this.context.getBean(JobExplorer.class));
		assertEquals(0, new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from BATCH_JOB_EXECUTION").size());
	}

	@Test
	public void testDefinesAndLaunchesJob() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JobConfiguration.class, BatchAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(JobLauncher.class));
		this.context.getBean(JobLauncherCommandLineRunner.class).run();
		assertNotNull(this.context.getBean(JobRepository.class).getLastJobExecution(
				"job", new JobParameters()));
	}

	@Test
	public void testDisableLaunchesJob() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		SpringBootTestUtils.addEnviroment(this.context, "spring.batch.job.enabled:false");
		this.context.register(JobConfiguration.class, BatchAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(JobLauncher.class));
		assertEquals(0, this.context.getBeanNamesForType(CommandLineRunner.class).length);
	}

	@Test
	public void testDisableSchemaLoader() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		SpringBootTestUtils.addEnviroment(this.context, "spring.datasource.name:batchtest",
				"spring.batch.initializer.enabled:false");
		this.context.register(TestConfiguration.class, BatchAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(JobLauncher.class));
		this.expected.expect(BadSqlGrammarException.class);
		new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from BATCH_JOB_EXECUTION");
	}

	@Test
	public void testUsingJpa() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		// The order is very important here: DataSource -> Hibernate -> Batch
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				ComponentScanDetectorConfiguration.class,
				HibernateJpaAutoConfiguration.class, BatchAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		PlatformTransactionManager transactionManager = this.context
				.getBean(PlatformTransactionManager.class);
		// It's a lazy proxy, but it does render its target if you ask for toString():
		assertTrue(transactionManager.toString().contains("JpaTransactionManager"));
		assertNotNull(this.context.getBean(EntityManagerFactory.class));
		// Ensure the JobRepository can be used (no problem with isolation level)
		assertNull(this.context.getBean(JobRepository.class).getLastJobExecution("job",
				new JobParameters()));
	}

	@EnableBatchProcessing
	@ComponentScan(basePackageClasses = City.class)
	protected static class TestConfiguration {
	}

	@EnableBatchProcessing
	protected static class JobConfiguration {
		@Autowired
		private JobRepository jobRepository;

		@Bean
		public Job job() {
			AbstractJob job = new AbstractJob() {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution)
						throws JobExecutionException {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}
	}

}
