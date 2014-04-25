/*
 * Copyright 2012-2014 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link JobLauncherCommandLineRunner}.
 * 
 * @author Dave Syer
 */
public class JobLauncherCommandLineRunnerTests {

	private JobLauncherCommandLineRunner runner;

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private JobExplorer jobExplorer;

	private JobLauncher jobLauncher;

	private JobBuilderFactory jobs;

	private StepBuilderFactory steps;

	private Job job;

	private Step step;

	@Before
	public void init() throws Exception {
		this.context.register(BatchConfiguration.class);
		this.context.refresh();
		JobRepository jobRepository = this.context.getBean(JobRepository.class);
		this.jobLauncher = this.context.getBean(JobLauncher.class);
		this.jobs = new JobBuilderFactory(jobRepository);
		PlatformTransactionManager transactionManager = this.context
				.getBean(PlatformTransactionManager.class);
		this.steps = new StepBuilderFactory(jobRepository, transactionManager);
		this.step = this.steps.get("step").tasklet(new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution contribution,
					ChunkContext chunkContext) throws Exception {
				return null;
			}
		}).build();
		this.job = this.jobs.get("job").start(this.step).build();
		this.jobExplorer = this.context.getBean(JobExplorer.class);
		this.runner = new JobLauncherCommandLineRunner(this.jobLauncher, this.jobExplorer);
		this.context.getBean(BatchConfiguration.class).clear();
	}

	@Test
	public void basicExecution() throws Exception {
		this.runner.execute(this.job, new JobParameters());
		assertEquals(1, this.jobExplorer.getJobInstances("job", 0, 100).size());
		this.runner.execute(this.job, new JobParametersBuilder().addLong("id", 1L)
				.toJobParameters());
		assertEquals(2, this.jobExplorer.getJobInstances("job", 0, 100).size());
	}

	@Test
	public void incrementExistingExecution() throws Exception {
		this.job = this.jobs.get("job").start(this.step)
				.incrementer(new RunIdIncrementer()).build();
		this.runner.execute(this.job, new JobParameters());
		this.runner.execute(this.job, new JobParameters());
		assertEquals(2, this.jobExplorer.getJobInstances("job", 0, 100).size());
	}

	@Test
	public void retryFailedExecution() throws Exception {
		this.job = this.jobs.get("job")
				.start(this.steps.get("step").tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution,
							ChunkContext chunkContext) throws Exception {
						throw new RuntimeException("Planned");
					}
				}).build()).incrementer(new RunIdIncrementer()).build();
		this.runner.execute(this.job, new JobParameters());
		this.runner.execute(this.job, new JobParameters());
		assertEquals(1, this.jobExplorer.getJobInstances("job", 0, 100).size());
	}

	@Test
	public void retryFailedExecutionWithNonIdentifyingParameters() throws Exception {
		this.job = this.jobs.get("job")
				.start(this.steps.get("step").tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution,
							ChunkContext chunkContext) throws Exception {
						throw new RuntimeException("Planned");
					}
				}).build()).incrementer(new RunIdIncrementer()).build();
		JobParameters jobParameters = new JobParametersBuilder().addLong("id", 1L, false)
				.toJobParameters();
		this.runner.execute(this.job, jobParameters);
		this.runner.execute(this.job, jobParameters);
		assertEquals(1, this.jobExplorer.getJobInstances("job", 0, 100).size());
	}

	@Configuration
	@EnableBatchProcessing
	protected static class BatchConfiguration implements BatchConfigurer {

		private ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();
		private JobRepository jobRepository;
		private MapJobRepositoryFactoryBean jobRepositoryFactory = new MapJobRepositoryFactoryBean(
				this.transactionManager);

		public BatchConfiguration() throws Exception {
			this.jobRepository = this.jobRepositoryFactory.getJobRepository();
		}

		public void clear() {
			this.jobRepositoryFactory.clear();
		}

		@Override
		public JobRepository getJobRepository() throws Exception {
			return this.jobRepository;
		}

		@Override
		public PlatformTransactionManager getTransactionManager() throws Exception {
			return this.transactionManager;
		}

		@Override
		public JobLauncher getJobLauncher() throws Exception {
			SimpleJobLauncher launcher = new SimpleJobLauncher();
			launcher.setJobRepository(this.jobRepository);
			launcher.setTaskExecutor(new SyncTaskExecutor());
			return launcher;
		}

		@Bean
		public JobExplorer jobExplorer() throws Exception {
			return new MapJobExplorerFactoryBean(this.jobRepositoryFactory).getObject();
		}
	}

}
