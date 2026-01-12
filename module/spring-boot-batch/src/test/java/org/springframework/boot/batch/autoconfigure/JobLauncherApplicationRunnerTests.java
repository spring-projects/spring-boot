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

package org.springframework.boot.batch.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

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
		.withBean(PlatformTransactionManager.class, ResourcelessTransactionManager::new)
		.withUserConfiguration(BatchConfiguration.class);

	@Test
	void basicExecutionSuccess() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			jobLauncherContext.executeJob(new JobParameters());
			List<JobInstance> jobInstances = jobLauncherContext.jobInstances();
			assertThat(jobInstances).hasSize(1);
			List<JobExecution> jobExecutions = jobLauncherContext.jobExecutions(jobInstances.get(0));
			assertThat(jobExecutions).hasSize(1);
			assertThat(jobExecutions.get(0).getExitStatus().getExitCode())
				.isEqualTo(ExitStatus.COMPLETED.getExitCode());
		});
	}

	@Test
	void basicExecutionFailure() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
			Job job = jobLauncherContext.jobBuilder()
				.start(jobLauncherContext.stepBuilder().tasklet(throwingTasklet(), transactionManager).build())
				.build();
			jobLauncherContext.runner.execute(job, new JobParameters());
			List<JobInstance> jobInstances = jobLauncherContext.jobInstances();
			assertThat(jobInstances).hasSize(1);
			List<JobExecution> jobExecutions = jobLauncherContext.jobExecutions(jobInstances.get(0));
			assertThat(jobExecutions).hasSize(1);
			assertThat(jobExecutions.get(0).getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
		});
	}

	private Tasklet throwingTasklet() {
		return (contribution, chunkContext) -> {
			throw new RuntimeException("Planned");
		};
	}

	static class JobLauncherApplicationRunnerContext {

		private final JobLauncherApplicationRunner runner;

		private final JobRepository jobRepository;

		private final JobBuilder jobBuilder;

		private final Job job;

		private final StepBuilder stepBuilder;

		JobLauncherApplicationRunnerContext(ApplicationContext context) {
			JobOperator jobOperator = context.getBean(JobOperator.class);
			JobRepository jobRepository = context.getBean(JobRepository.class);
			PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
			this.stepBuilder = new StepBuilder("step", jobRepository);
			Step step = this.stepBuilder.tasklet((contribution, chunkContext) -> null, transactionManager).build();
			this.jobBuilder = new JobBuilder("job", jobRepository);
			this.job = this.jobBuilder.start(step).build();
			this.jobRepository = context.getBean(JobRepository.class);
			this.runner = new JobLauncherApplicationRunner(jobOperator);
		}

		List<JobInstance> jobInstances() {
			return this.jobRepository.getJobInstances("job", 0, 100);
		}

		List<JobExecution> jobExecutions(JobInstance jobInstance) {
			return this.jobRepository.getJobExecutions(jobInstance);
		}

		void executeJob(JobParameters jobParameters) throws JobExecutionException {
			this.runner.execute(this.job, jobParameters);
		}

		JobBuilder jobBuilder() {
			return this.jobBuilder;
		}

		StepBuilder stepBuilder() {
			return this.stepBuilder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	static class BatchConfiguration {

	}

}
