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

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BatchJobLauncherAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class BatchJobLauncherAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(BatchAutoConfiguration.class, BatchJobLauncherAutoConfiguration.class));

	@Test
	void testDefinesAndLaunchesJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JobOperator.class);
			context.getBean(JobLauncherApplicationRunner.class).run(new DefaultApplicationArguments("jobParam=test"));
			JobParameters jobParameters = new JobParametersBuilder().addString("jobParam", "test").toJobParameters();
			assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters)).isNotNull();
		});
	}

	@Test
	void testDefinesAndLaunchesJobIgnoreOptionArguments() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JobOperator.class);
			context.getBean(JobLauncherApplicationRunner.class)
				.run(new DefaultApplicationArguments("--spring.property=value", "jobParam=test"));
			JobParameters jobParameters = new JobParametersBuilder().addString("jobParam", "test").toJobParameters();
			assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters)).isNotNull();
		});
	}

	@Test
	void testRegisteredAndLocalJob() {
		this.contextRunner.withUserConfiguration(NamedJobConfigurationWithRegisteredAndLocalJob.class)
			.withPropertyValues("spring.batch.job.name:discreteRegisteredJob")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				context.getBean(JobLauncherApplicationRunner.class).run();
				JobExecution lastJobExecution = context.getBean(JobRepository.class)
					.getLastJobExecution("discreteRegisteredJob", new JobParameters());
				assertThat(lastJobExecution).isNotNull();
				assertThat(lastJobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
			});
	}

	@Test
	void testDefinesAndLaunchesLocalJob() {
		this.contextRunner.withUserConfiguration(NamedJobConfigurationWithLocalJob.class)
			.withPropertyValues("spring.batch.job.name:discreteLocalJob")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				context.getBean(JobLauncherApplicationRunner.class).run();
				assertThat(context.getBean(JobRepository.class)
					.getLastJobExecution("discreteLocalJob", new JobParameters())).isNotNull();
			});
	}

	@Test
	void testMultipleJobsAndNoJobName() {
		this.contextRunner.withUserConfiguration(MultipleJobConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			Throwable startupFailure = context.getStartupFailure();
			assertThat(startupFailure).isNotNull();
			Throwable cause = startupFailure.getCause();
			assertThat(cause).isNotNull();
			assertThat(cause.getMessage()).contains("Job name must be specified in case of multiple jobs");
		});
	}

	@Test
	void testMultipleJobsAndJobName() {
		this.contextRunner.withUserConfiguration(MultipleJobConfiguration.class)
			.withPropertyValues("spring.batch.job.name:discreteLocalJob")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				context.getBean(JobLauncherApplicationRunner.class).run();
				assertThat(context.getBean(JobRepository.class)
					.getLastJobExecution("discreteLocalJob", new JobParameters())).isNotNull();
			});
	}

	@Test
	void testDisableLaunchesJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class)
			.withPropertyValues("spring.batch.job.enabled:false")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				assertThat(context).doesNotHaveBean(CommandLineRunner.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class NamedJobConfigurationWithRegisteredAndLocalJob {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteRegisteredJob") {

				private static int count;

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return mock(Step.class);
				}

				@Override
				protected void doExecute(JobExecution execution) {
					if (count == 0) {
						execution.setStatus(BatchStatus.COMPLETED);
					}
					else {
						execution.setStatus(BatchStatus.FAILED);
					}
					count++;
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NamedJobConfigurationWithLocalJob {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteLocalJob") {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return mock(Step.class);
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleJobConfiguration {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteLocalJob") {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return mock(Step.class);
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

		@Bean
		Job job2() {
			return new Job() {
				@Override
				public String getName() {
					return "discreteLocalJob2";
				}

				@Override
				public void execute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JobConfiguration {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job job() {
			AbstractJob job = new AbstractJob() {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return mock(Step.class);
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

}
