/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.task;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.task.TaskSchedulerCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TaskSchedulingAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class TaskSchedulingAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class).withConfiguration(
					AutoConfigurations.of(TaskSchedulingAutoConfiguration.class));

	@Test
	public void noSchedulingDoesNotExposeTaskScheduler() {
		this.contextRunner.run(
				(context) -> assertThat(context).doesNotHaveBean(TaskScheduler.class));
	}

	@Test
	public void enableSchedulingWithNoTaskExecutorAutoConfiguresOne() {
		this.contextRunner
				.withPropertyValues(
						"spring.task.scheduling.shutdown.await-termination=true",
						"spring.task.scheduling.shutdown.await-termination-period=30s",
						"spring.task.scheduling.thread-name-prefix=scheduling-test-")
				.withUserConfiguration(SchedulingConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutor.class);
					TaskExecutor taskExecutor = context.getBean(TaskExecutor.class);
					TestBean bean = context.getBean(TestBean.class);
					Thread.sleep(15);
					assertThat(taskExecutor).hasFieldOrPropertyWithValue(
							"waitForTasksToCompleteOnShutdown", true);
					assertThat(taskExecutor)
							.hasFieldOrPropertyWithValue("awaitTerminationSeconds", 30);
					assertThat(bean.threadNames)
							.allMatch((name) -> name.contains("scheduling-test-"));
				});
	}

	@Test
	public void enableSchedulingWithNoTaskExecutorAppliesCustomizers() {
		this.contextRunner
				.withPropertyValues(
						"spring.task.scheduling.thread-name-prefix=scheduling-test-")
				.withUserConfiguration(SchedulingConfiguration.class,
						TaskSchedulerCustomizerConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutor.class);
					TestBean bean = context.getBean(TestBean.class);
					Thread.sleep(15);
					assertThat(bean.threadNames)
							.allMatch((name) -> name.contains("customized-scheduler-"));
				});
	}

	@Test
	public void enableSchedulingWithExistingTaskSchedulerBacksOff() {
		this.contextRunner.withUserConfiguration(SchedulingConfiguration.class,
				TaskSchedulerConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(TaskScheduler.class);
					assertThat(context.getBean(TaskScheduler.class))
							.isInstanceOf(TestTaskScheduler.class);
					TestBean bean = context.getBean(TestBean.class);
					Thread.sleep(15);
					assertThat(bean.threadNames).containsExactly("test-1");
				});
	}

	@Test
	public void enableSchedulingWithExistingScheduledExecutorServiceBacksOff() {
		this.contextRunner.withUserConfiguration(SchedulingConfiguration.class,
				ScheduledExecutorServiceConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean(TaskScheduler.class);
					assertThat(context).hasSingleBean(ScheduledExecutorService.class);
					TestBean bean = context.getBean(TestBean.class);
					Thread.sleep(15);
					assertThat(bean.threadNames)
							.allMatch((name) -> name.contains("pool-"));
				});
	}

	@Test
	public void enableSchedulingWithConfigurerBacksOff() {
		this.contextRunner.withUserConfiguration(SchedulingConfiguration.class,
				SchedulingConfigurerConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean(TaskScheduler.class);
					TestBean bean = context.getBean(TestBean.class);
					Thread.sleep(15);
					assertThat(bean.threadNames).containsExactly("test-1");
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	static class SchedulingConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TaskSchedulerConfiguration {

		@Bean
		public TaskScheduler customTaskScheduler() {
			return new TestTaskScheduler();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ScheduledExecutorServiceConfiguration {

		@Bean
		public ScheduledExecutorService customScheduledExecutorService() {
			return Executors.newScheduledThreadPool(2);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TaskSchedulerCustomizerConfiguration {

		@Bean
		public TaskSchedulerCustomizer testTaskSchedulerCustomizer() {
			return ((taskScheduler) -> taskScheduler
					.setThreadNamePrefix("customized-scheduler-"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SchedulingConfigurerConfiguration implements SchedulingConfigurer {

		private final TaskScheduler taskScheduler = new TestTaskScheduler();

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.setScheduler(this.taskScheduler);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		public TestBean testBean() {
			return new TestBean();
		}

	}

	static class TestBean {

		private final Set<String> threadNames = ConcurrentHashMap.newKeySet();

		@Scheduled(fixedRate = 10)
		public void accumulate() {
			this.threadNames.add(Thread.currentThread().getName());
		}

	}

	static class TestTaskScheduler extends ThreadPoolTaskScheduler {

		TestTaskScheduler() {
			setPoolSize(1);
			setThreadNamePrefix("test-");
			afterPropertiesSet();
		}

	}

}
