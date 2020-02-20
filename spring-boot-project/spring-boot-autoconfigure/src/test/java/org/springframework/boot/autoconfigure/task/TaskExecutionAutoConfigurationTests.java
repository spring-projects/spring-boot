/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TaskExecutionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Camille Vienot
 */
@ExtendWith(OutputCaptureExtension.class)
class TaskExecutionAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class));

	@Test
	void taskExecutorBuilderShouldApplyCustomSettings() {
		this.contextRunner.withPropertyValues("spring.task.execution.pool.queue-capacity=10",
				"spring.task.execution.pool.core-size=2", "spring.task.execution.pool.max-size=4",
				"spring.task.execution.pool.allow-core-thread-timeout=true", "spring.task.execution.pool.keep-alive=5s",
				"spring.task.execution.shutdown.await-termination=true",
				"spring.task.execution.shutdown.await-termination-period=30s",
				"spring.task.execution.thread-name-prefix=mytest-").run(assertTaskExecutor((taskExecutor) -> {
					assertThat(taskExecutor).hasFieldOrPropertyWithValue("queueCapacity", 10);
					assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
					assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
					assertThat(taskExecutor).hasFieldOrPropertyWithValue("allowCoreThreadTimeOut", true);
					assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(5);
					assertThat(taskExecutor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
					assertThat(taskExecutor).hasFieldOrPropertyWithValue("awaitTerminationMillis", 30000L);
					assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
				}));
	}

	@Test
	void taskExecutorBuilderWhenHasCustomBuilderShouldUseCustomBuilder() {
		this.contextRunner.withUserConfiguration(CustomTaskExecutorBuilderConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
			assertThat(context.getBean(TaskExecutorBuilder.class))
					.isSameAs(context.getBean(CustomTaskExecutorBuilderConfig.class).taskExecutorBuilder);
		});
	}

	@Test
	void taskExecutorBuilderShouldUseTaskDecorator() {
		this.contextRunner.withUserConfiguration(TaskDecoratorConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
			ThreadPoolTaskExecutor executor = context.getBean(TaskExecutorBuilder.class).build();
			assertThat(ReflectionTestUtils.getField(executor, "taskDecorator"))
					.isSameAs(context.getBean(TaskDecorator.class));
		});
	}

	@Test
	void taskExecutorAutoConfigured(CapturedOutput output) {
		this.contextRunner.run((context) -> {
			assertThat(output).doesNotContain("Initializing ExecutorService");
			assertThat(context).hasSingleBean(Executor.class);
			assertThat(context).hasBean("applicationTaskExecutor");
			assertThat(context).getBean("applicationTaskExecutor").isInstanceOf(ThreadPoolTaskExecutor.class);
			assertThat(output).contains("Initializing ExecutorService");
		});
	}

	@Test
	void taskExecutorWhenHasCustomTaskExecutorShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomTaskExecutorConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(Executor.class);
			assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
		});
	}

	@Test
	void taskExecutorBuilderShouldApplyCustomizer() {
		this.contextRunner.withUserConfiguration(TaskExecutorCustomizerConfig.class).run((context) -> {
			TaskExecutorCustomizer customizer = context.getBean(TaskExecutorCustomizer.class);
			ThreadPoolTaskExecutor executor = context.getBean(TaskExecutorBuilder.class).build();
			verify(customizer).customize(executor);
		});
	}

	@Test
	void enableAsyncUsesAutoConfiguredOneByDefault() {
		this.contextRunner.withPropertyValues("spring.task.execution.thread-name-prefix=task-test-")
				.withUserConfiguration(AsyncConfiguration.class, TestBean.class).run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutor.class);
					TestBean bean = context.getBean(TestBean.class);
					String text = bean.echo("something").get();
					assertThat(text).contains("task-test-").contains("something");
				});
	}

	@Test
	void enableAsyncUsesAutoConfiguredOneByDefaultEvenThoughSchedulingIsConfigured() {
		this.contextRunner.withPropertyValues("spring.task.execution.thread-name-prefix=task-test-")
				.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
				.withUserConfiguration(AsyncConfiguration.class, SchedulingConfiguration.class, TestBean.class)
				.run((context) -> {
					TestBean bean = context.getBean(TestBean.class);
					String text = bean.echo("something").get();
					assertThat(text).contains("task-test-").contains("something");
				});
	}

	private ContextConsumer<AssertableApplicationContext> assertTaskExecutor(
			Consumer<ThreadPoolTaskExecutor> taskExecutor) {
		return (context) -> {
			assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
			TaskExecutorBuilder builder = context.getBean(TaskExecutorBuilder.class);
			taskExecutor.accept(builder.build());
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTaskExecutorBuilderConfig {

		private final TaskExecutorBuilder taskExecutorBuilder = new TaskExecutorBuilder();

		@Bean
		TaskExecutorBuilder customTaskExecutorBuilder() {
			return this.taskExecutorBuilder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TaskExecutorCustomizerConfig {

		@Bean
		TaskExecutorCustomizer mockTaskExecutorCustomizer() {
			return mock(TaskExecutorCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TaskDecoratorConfig {

		@Bean
		TaskDecorator mockTaskDecorator() {
			return mock(TaskDecorator.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTaskExecutorConfig {

		@Bean
		Executor customTaskExecutor() {
			return new SyncTaskExecutor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAsync
	static class AsyncConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	static class SchedulingConfiguration {

	}

	static class TestBean {

		@Async
		Future<String> echo(String text) {
			return new AsyncResult<>(Thread.currentThread().getName() + " " + text);
		}

	}

}
