/*
 * Copyright 2012-2021 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
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
class TaskSchedulingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class)
			.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class));

	@Test
	void noSchedulingDoesNotExposeTaskScheduler() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(TaskScheduler.class));
	}

	@Test
	void enableSchedulingWithNoTaskExecutorAutoConfiguresOne() {
		this.contextRunner
				.withPropertyValues("spring.task.scheduling.shutdown.await-termination=true",
						"spring.task.scheduling.shutdown.await-termination-period=30s",
						"spring.task.scheduling.thread-name-prefix=scheduling-test-")
				.withUserConfiguration(SchedulingConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutor.class);
					TaskExecutor taskExecutor = context.getBean(TaskExecutor.class);
					TestBean bean = context.getBean(TestBean.class);
					assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
					assertThat(taskExecutor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
					assertThat(taskExecutor).hasFieldOrPropertyWithValue("awaitTerminationMillis", 30000L);
					assertThat(bean.threadNames).allMatch((name) -> name.contains("scheduling-test-"));
				});
	}

	@Test
	void enableSchedulingWithNoTaskExecutorAppliesCustomizers() {
		this.contextRunner.withPropertyValues("spring.task.scheduling.thread-name-prefix=scheduling-test-")
				.withUserConfiguration(SchedulingConfiguration.class, TaskSchedulerCustomizerConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutor.class);
					TestBean bean = context.getBean(TestBean.class);
					assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
					assertThat(bean.threadNames).allMatch((name) -> name.contains("customized-scheduler-"));
				});
	}

	@Test
	void enableSchedulingWithExistingTaskSchedulerBacksOff() {
		this.contextRunner.withUserConfiguration(SchedulingConfiguration.class, TaskSchedulerConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(TaskScheduler.class);
					assertThat(context.getBean(TaskScheduler.class)).isInstanceOf(TestTaskScheduler.class);
					TestBean bean = context.getBean(TestBean.class);
					assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
					assertThat(bean.threadNames).containsExactly("test-1");
				});
	}

	@Test
	void enableSchedulingWithExistingScheduledExecutorServiceBacksOff() {
		this.contextRunner
				.withUserConfiguration(SchedulingConfiguration.class, ScheduledExecutorServiceConfiguration.class)
				.run((context) -> {
					assertThat(context).doesNotHaveBean(TaskScheduler.class);
					assertThat(context).hasSingleBean(ScheduledExecutorService.class);
					TestBean bean = context.getBean(TestBean.class);
					assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
					assertThat(bean.threadNames).allMatch((name) -> name.contains("pool-"));
				});
	}

	@Test
	void enableSchedulingWithConfigurerBacksOff() {
		this.contextRunner.withUserConfiguration(SchedulingConfiguration.class, SchedulingConfigurerConfiguration.class)
				.run((context) -> {
					assertThat(context).doesNotHaveBean(TaskScheduler.class);
					TestBean bean = context.getBean(TestBean.class);
					assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
					assertThat(bean.threadNames).containsExactly("test-1");
				});
	}

	@Test
	void enableSchedulingWithLazyInitializationInvokeScheduledMethods() {
		List<String> threadNames = new ArrayList<>();
		new ApplicationContextRunner()
				.withInitializer((context) -> context
						.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
				.withPropertyValues("spring.task.scheduling.thread-name-prefix=scheduling-test-")
				.withBean(LazyTestBean.class, () -> new LazyTestBean(threadNames))
				.withUserConfiguration(SchedulingConfiguration.class)
				.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class)).run((context) -> {
					// No lazy lookup.
					Awaitility.waitAtMost(Duration.ofSeconds(3)).until(() -> !threadNames.isEmpty());
					assertThat(threadNames).allMatch((name) -> name.contains("scheduling-test-"));
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	static class SchedulingConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TaskSchedulerConfiguration {

		@Bean
		TaskScheduler customTaskScheduler() {
			return new TestTaskScheduler();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ScheduledExecutorServiceConfiguration {

		@Bean
		ScheduledExecutorService customScheduledExecutorService() {
			return Executors.newScheduledThreadPool(2);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TaskSchedulerCustomizerConfiguration {

		@Bean
		TaskSchedulerCustomizer testTaskSchedulerCustomizer() {
			return ((taskScheduler) -> taskScheduler.setThreadNamePrefix("customized-scheduler-"));
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
		TestBean testBean() {
			return new TestBean();
		}

	}

	static class TestBean {

		private final Set<String> threadNames = ConcurrentHashMap.newKeySet();

		private final CountDownLatch latch = new CountDownLatch(1);

		@Scheduled(fixedRate = 60000)
		void accumulate() {
			this.threadNames.add(Thread.currentThread().getName());
			this.latch.countDown();
		}

	}

	static class LazyTestBean {

		private final List<String> threadNames;

		LazyTestBean(List<String> threadNames) {
			this.threadNames = threadNames;
		}

		@Scheduled(fixedRate = 2000)
		void accumulate() {
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
