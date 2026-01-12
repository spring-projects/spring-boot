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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.converter.JsonJobParametersConverter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration.SpringBootBatchDefaultConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BatchJobLauncherAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Kazuki Shimizu
 * @author Mahmoud Ben Hassine
 * @author Lars Uffmann
 * @author Lasse Wulff
 * @author Yanming Zhou
 */
@ExtendWith(OutputCaptureExtension.class)
class BatchAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BatchAutoConfiguration.class));

	@Test
	void testDefaultContext() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(JobRepository.class);
			assertThat(context).hasSingleBean(JobOperator.class);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserEnablesBatchProcessing() {
		this.contextRunner.withUserConfiguration(EnableBatchProcessingConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(SpringBootBatchDefaultConfiguration.class));
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesBatchConfiguration() {
		this.contextRunner.withUserConfiguration(CustomBatchConfiguration.class)
			.withClassLoader(new FilteredClassLoader(DatabasePopulator.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SpringBootBatchDefaultConfiguration.class));
	}

	@Test
	void testBatchTaskExecutor() {
		this.contextRunner.withUserConfiguration(BatchTaskExecutorConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(SpringBootBatchDefaultConfiguration.class).hasBean("batchTaskExecutor");
			TaskExecutor batchTaskExecutor = context.getBean("batchTaskExecutor", TaskExecutor.class);
			assertThat(batchTaskExecutor).isInstanceOf(AsyncTaskExecutor.class);
			assertThat(context.getBean(SpringBootBatchDefaultConfiguration.class).getTaskExecutor())
				.isEqualTo(batchTaskExecutor);
			JobOperator jobOperator = AopTestUtils.getTargetObject(context.getBean(JobOperator.class));
			assertThat(jobOperator).hasFieldOrPropertyWithValue("taskExecutor", batchTaskExecutor);
		});
	}

	@Test
	void whenTheUserDefinesAJobNameAsJobInstanceValidates() {
		JobLauncherApplicationRunner runner = createInstance("another");
		runner.setJobs(Collections.singletonList(mockJob("test")));
		runner.setJobName("test");
		runner.afterPropertiesSet();
	}

	@Test
	void whenTheUserDefinesAJobNameAsRegisteredJobValidates() {
		JobLauncherApplicationRunner runner = createInstance("test");
		runner.setJobName("test");
		runner.afterPropertiesSet();
	}

	@Test
	void whenTheUserDefinesAJobNameThatDoesNotExistWithJobInstancesFailsFast() {
		JobLauncherApplicationRunner runner = createInstance();
		runner.setJobs(Arrays.asList(mockJob("one"), mockJob("two")));
		runner.setJobName("three");
		assertThatIllegalStateException().isThrownBy(runner::afterPropertiesSet)
			.withMessage("No job found with name 'three'");
	}

	@Test
	void whenTheUserDefinesAJobNameThatDoesNotExistWithRegisteredJobFailsFast() {
		JobLauncherApplicationRunner runner = createInstance("one", "two");
		runner.setJobName("three");
		assertThatIllegalStateException().isThrownBy(runner::afterPropertiesSet)
			.withMessage("No job found with name 'three'");
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void customJobParametersConverterIsUsed() {
		this.contextRunner.withBean(JobParametersConverter.class, JsonJobParametersConverter::new).run((context) -> {
			assertThat(context).hasSingleBean(JsonJobParametersConverter.class);
			assertThat(context.getBean(SpringBootBatchDefaultConfiguration.class).getJobParametersConverter())
				.isInstanceOf(JsonJobParametersConverter.class);
		});
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void defaultJobParametersConverterIsUsed() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(JobParametersConverter.class);
			assertThat(context.getBean(SpringBootBatchDefaultConfiguration.class).getJobParametersConverter())
				.isInstanceOf(DefaultJobParametersConverter.class);
		});
	}

	private JobLauncherApplicationRunner createInstance(String... registeredJobNames) {
		JobLauncherApplicationRunner runner = new JobLauncherApplicationRunner(mock(JobOperator.class));
		JobRegistry jobRegistry = mock(JobRegistry.class);
		given(jobRegistry.getJobNames()).willReturn(Arrays.asList(registeredJobNames));
		runner.setJobRegistry(jobRegistry);
		return runner;
	}

	private Job mockJob(String name) {
		Job job = mock(Job.class);
		given(job.getName()).willReturn(name);
		return job;
	}

	@Configuration(proxyBeanMethods = false)
	static class BatchTaskExecutorConfiguration {

		@Bean
		TaskExecutor taskExecutor() {
			return new SyncTaskExecutor();
		}

		@BatchTaskExecutor
		@Bean(defaultCandidate = false)
		TaskExecutor batchTaskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomBatchConfiguration extends DefaultBatchConfiguration {

	}

	@EnableBatchProcessing
	@Configuration(proxyBeanMethods = false)
	static class EnableBatchProcessingConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ConversionServiceCustomizersConfiguration {

		@Bean
		@Order(1)
		BatchConversionServiceCustomizer batchConversionServiceCustomizer() {
			return mock(BatchConversionServiceCustomizer.class);
		}

		@Bean
		@Order(2)
		BatchConversionServiceCustomizer anotherBatchConversionServiceCustomizer() {
			return mock(BatchConversionServiceCustomizer.class);
		}

	}

}
