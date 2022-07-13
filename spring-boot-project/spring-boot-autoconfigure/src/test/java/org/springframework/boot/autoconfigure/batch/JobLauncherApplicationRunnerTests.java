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

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link JobLauncherApplicationRunner}.
 *
 * @author Dave Syer
 * @author Jean-Pierre Bergamin
 * @author Mahmoud Ben Hassine
 * @author Stephane Nicoll
 */
class JobLauncherApplicationRunnerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class))
			.withUserConfiguration(BatchConfiguration.class);

	@Test
	void basicExecution() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			jobLauncherContext.executeJob(new JobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(1);
			jobLauncherContext.executeJob(new JobParametersBuilder().addLong("id", 1L).toJobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
		});
	}

	@Test
	void incrementExistingExecution() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.configureJob().incrementer(new RunIdIncrementer()).build();
			jobLauncherContext.runner.execute(job, new JobParameters());
			jobLauncherContext.runner.execute(job, new JobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
		});
	}

	@Test
	void retryFailedExecution() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.jobBuilder()
					.start(jobLauncherContext.stepBuilder().tasklet(throwingTasklet()).build())
					.incrementer(new RunIdIncrementer()).build();
			jobLauncherContext.runner.execute(job, new JobParameters());
			jobLauncherContext.runner.execute(job, new JobParametersBuilder().addLong("run.id", 1L).toJobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(1);
		});
	}

	@Test
	void runDifferentInstances() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.jobBuilder()
					.start(jobLauncherContext.stepBuilder().tasklet(throwingTasklet()).build()).build();
			// start a job instance
			JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo").toJobParameters();
			jobLauncherContext.runner.execute(job, jobParameters);
			assertThat(jobLauncherContext.jobInstances()).hasSize(1);
			// start a different job instance
			JobParameters otherJobParameters = new JobParametersBuilder().addString("name", "bar").toJobParameters();
			jobLauncherContext.runner.execute(job, otherJobParameters);
			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
		});
	}

	@Test
	void retryFailedExecutionOnNonRestartableJob() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.jobBuilder().preventRestart()
					.start(jobLauncherContext.stepBuilder().tasklet(throwingTasklet()).build())
					.incrementer(new RunIdIncrementer()).build();
			jobLauncherContext.runner.execute(job, new JobParameters());
			jobLauncherContext.runner.execute(job, new JobParameters());
			// A failed job that is not restartable does not re-use the job params of
			// the last execution, but creates a new job instance when running it again.
			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
			assertThatExceptionOfType(JobRestartException.class).isThrownBy(() -> {
				// try to re-run a failed execution
				jobLauncherContext.runner.execute(job,
						new JobParametersBuilder().addLong("run.id", 1L).toJobParameters());
				fail("expected JobRestartException");
			}).withMessageContaining("JobInstance already exists and is not restartable");
		});
	}

	@Test
	void retryFailedExecutionWithNonIdentifyingParameters() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.jobBuilder()
					.start(jobLauncherContext.stepBuilder().tasklet(throwingTasklet()).build())
					.incrementer(new RunIdIncrementer()).build();
			JobParameters jobParameters = new JobParametersBuilder().addLong("id", 1L, false).addLong("foo", 2L, false)
					.toJobParameters();
			jobLauncherContext.runner.execute(job, jobParameters);
			assertThat(jobLauncherContext.jobInstances()).hasSize(1);
			// try to re-run a failed execution with non identifying parameters
			jobLauncherContext.runner.execute(job,
					new JobParametersBuilder(jobParameters).addLong("run.id", 1L).toJobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(1);
		});
	}

	private Tasklet throwingTasklet() {
		return (contribution, chunkContext) -> {
			throw new RuntimeException("Planned");
		};
	}

	static class JobLauncherApplicationRunnerContext {

		private final JobLauncherApplicationRunner runner;

		private final JobExplorer jobExplorer;

		private final JobBuilderFactory jobs;

		private final StepBuilderFactory steps;

		private final Job job;

		private final Step step;

		JobLauncherApplicationRunnerContext(ApplicationContext context) {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);
			JobRepository jobRepository = context.getBean(JobRepository.class);
			this.jobs = new JobBuilderFactory(jobRepository);
			this.steps = new StepBuilderFactory(jobRepository, context.getBean(PlatformTransactionManager.class));
			this.step = this.steps.get("step").tasklet((contribution, chunkContext) -> null).build();
			this.job = this.jobs.get("job").start(this.step).build();
			this.jobExplorer = context.getBean(JobExplorer.class);
			this.runner = new JobLauncherApplicationRunner(jobLauncher, this.jobExplorer, jobRepository);
		}

		List<JobInstance> jobInstances() {
			return this.jobExplorer.getJobInstances("job", 0, 100);
		}

		void executeJob(JobParameters jobParameters) throws JobExecutionException {
			this.runner.execute(this.job, jobParameters);
		}

		JobBuilder jobBuilder() {
			return this.jobs.get("job");
		}

		StepBuilder stepBuilder() {
			return this.steps.get("step");
		}

		SimpleJobBuilder configureJob() {
			return this.jobs.get("job").start(this.step);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	static class BatchConfiguration extends BasicBatchConfigurer {

		private final DataSource dataSource;

		protected BatchConfiguration(DataSource dataSource) {
			super(new BatchProperties(), dataSource, new TransactionManagerCustomizers(null));
			this.dataSource = dataSource;
		}

		@Bean
		DataSourceScriptDatabaseInitializer batchDataSourceInitializer() {
			DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
			settings.setSchemaLocations(Arrays.asList("classpath:org/springframework/batch/core/schema-h2.sql"));
			return new DataSourceScriptDatabaseInitializer(this.dataSource, settings);
		}

	}

}
