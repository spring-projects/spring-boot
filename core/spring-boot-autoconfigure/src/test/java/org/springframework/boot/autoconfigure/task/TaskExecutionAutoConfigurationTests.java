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

package org.springframework.boot.autoconfigure.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TaskExecutionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Camille Vienot
 * @author Moritz Halbritter
 * @author Yanming Zhou
 */
@ExtendWith(OutputCaptureExtension.class)
class TaskExecutionAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
			assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
			assertThat(context).hasSingleBean(SimpleAsyncTaskExecutorBuilder.class);
		});
	}

	@Test
	void simpleAsyncTaskExecutorBuilderShouldReadProperties() {
		this.contextRunner
			.withPropertyValues("spring.task.execution.thread-name-prefix=mytest-",
					"spring.task.execution.simple.reject-tasks-when-limit-reached=true",
					"spring.task.execution.simple.concurrency-limit=1",
					"spring.task.execution.shutdown.await-termination=true",
					"spring.task.execution.shutdown.await-termination-period=30s")
			.run(assertSimpleAsyncTaskExecutor((taskExecutor) -> {
				assertThat(taskExecutor).hasFieldOrPropertyWithValue("rejectTasksWhenLimitReached", true);
				assertThat(taskExecutor.getConcurrencyLimit()).isEqualTo(1);
				assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
				assertThat(taskExecutor).hasFieldOrPropertyWithValue("taskTerminationTimeout", 30000L);
			}));
	}

	@Test
	void threadPoolTaskExecutorBuilderShouldApplyCustomSettings() {
		this.contextRunner.withPropertyValues("spring.task.execution.pool.queue-capacity=10",
				"spring.task.execution.pool.core-size=2", "spring.task.execution.pool.max-size=4",
				"spring.task.execution.pool.allow-core-thread-timeout=true", "spring.task.execution.pool.keep-alive=5s",
				"spring.task.execution.pool.shutdown.accept-tasks-after-context-close=true",
				"spring.task.execution.shutdown.await-termination=true",
				"spring.task.execution.shutdown.await-termination-period=30s",
				"spring.task.execution.thread-name-prefix=mytest-")
			.run(assertThreadPoolTaskExecutor((taskExecutor) -> {
				assertThat(taskExecutor).hasFieldOrPropertyWithValue("queueCapacity", 10);
				assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
				assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
				assertThat(taskExecutor).hasFieldOrPropertyWithValue("allowCoreThreadTimeOut", true);
				assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(5);
				assertThat(taskExecutor).hasFieldOrPropertyWithValue("acceptTasksAfterContextClose", true);
				assertThat(taskExecutor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
				assertThat(taskExecutor).hasFieldOrPropertyWithValue("awaitTerminationMillis", 30000L);
				assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
			}));
	}

	@Test
	void threadPoolTaskExecutorBuilderWhenHasCustomBuilderShouldUseCustomBuilder() {
		this.contextRunner.withUserConfiguration(CustomThreadPoolTaskExecutorBuilderConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
			assertThat(context.getBean(ThreadPoolTaskExecutorBuilder.class))
				.isSameAs(context.getBean(CustomThreadPoolTaskExecutorBuilderConfig.class).builder);
		});
	}

	@Test
	void threadPoolTaskExecutorBuilderShouldUseTaskDecorator() {
		this.contextRunner.withBean(TaskDecorator.class, OrderedTaskDecorator::new).run((context) -> {
			assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
			ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutorBuilder.class).build();
			assertThat(executor).extracting("taskDecorator").isSameAs(context.getBean(TaskDecorator.class));
		});
	}

	@Test
	void threadPoolTaskExecutorBuilderShouldUseCompositeTaskDecorator() {
		this.contextRunner.withBean("taskDecorator1", TaskDecorator.class, () -> new OrderedTaskDecorator(1))
			.withBean("taskDecorator2", TaskDecorator.class, () -> new OrderedTaskDecorator(3))
			.withBean("taskDecorator3", TaskDecorator.class, () -> new OrderedTaskDecorator(2))
			.run((context) -> {
				assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
				ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutorBuilder.class).build();
				assertThat(executor).extracting("taskDecorator")
					.isInstanceOf(CompositeTaskDecorator.class)
					.extracting("taskDecorators")
					.asInstanceOf(InstanceOfAssertFactories.list(TaskDecorator.class))
					.containsExactly(context.getBean("taskDecorator1", TaskDecorator.class),
							context.getBean("taskDecorator3", TaskDecorator.class),
							context.getBean("taskDecorator2", TaskDecorator.class));
			});
	}

	@Test
	void whenThreadPoolTaskExecutorIsAutoConfiguredThenItIsLazy() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Executor.class).hasBean("applicationTaskExecutor");
			BeanDefinition beanDefinition = context.getSourceApplicationContext()
				.getBeanFactory()
				.getBeanDefinition("applicationTaskExecutor");
			assertThat(beanDefinition.isLazyInit()).isTrue();
			assertThat(context).getBean("applicationTaskExecutor").isInstanceOf(ThreadPoolTaskExecutor.class);
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void whenVirtualThreadsAreEnabledThenSimpleAsyncTaskExecutorWithVirtualThreadsIsAutoConfigured() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true").run((context) -> {
			assertThat(context).hasSingleBean(Executor.class).hasBean("applicationTaskExecutor");
			assertThat(context).getBean("applicationTaskExecutor").isInstanceOf(SimpleAsyncTaskExecutor.class);
			SimpleAsyncTaskExecutor taskExecutor = context.getBean("applicationTaskExecutor",
					SimpleAsyncTaskExecutor.class);
			assertThat(virtualThreadName(taskExecutor)).startsWith("task-");
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void whenTaskNamePrefixIsConfiguredThenSimpleAsyncTaskExecutorWithVirtualThreadsUsesIt() {
		this.contextRunner
			.withPropertyValues("spring.threads.virtual.enabled=true",
					"spring.task.execution.thread-name-prefix=custom-")
			.run((context) -> {
				SimpleAsyncTaskExecutor taskExecutor = context.getBean("applicationTaskExecutor",
						SimpleAsyncTaskExecutor.class);
				assertThat(virtualThreadName(taskExecutor)).startsWith("custom-");
			});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void whenVirtualThreadsAreAvailableButNotEnabledThenThreadPoolTaskExecutorIsAutoConfigured() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Executor.class).hasBean("applicationTaskExecutor");
			assertThat(context).getBean("applicationTaskExecutor").isInstanceOf(ThreadPoolTaskExecutor.class);
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void whenTaskDecoratorIsDefinedThenSimpleAsyncTaskExecutorWithVirtualThreadsUsesIt() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true")
			.withBean(TaskDecorator.class, OrderedTaskDecorator::new)
			.run((context) -> {
				SimpleAsyncTaskExecutor executor = context.getBean(SimpleAsyncTaskExecutor.class);
				assertThat(executor).extracting("taskDecorator").isSameAs(context.getBean(TaskDecorator.class));
			});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void whenTaskDecoratorsAreDefinedThenSimpleAsyncTaskExecutorWithVirtualThreadsUsesThem() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true")
			.withBean("taskDecorator1", TaskDecorator.class, () -> new OrderedTaskDecorator(1))
			.withBean("taskDecorator2", TaskDecorator.class, () -> new OrderedTaskDecorator(3))
			.withBean("taskDecorator3", TaskDecorator.class, () -> new OrderedTaskDecorator(2))
			.run((context) -> {
				SimpleAsyncTaskExecutor executor = context.getBean(SimpleAsyncTaskExecutor.class);
				assertThat(executor).extracting("taskDecorator")
					.isInstanceOf(CompositeTaskDecorator.class)
					.extracting("taskDecorators")
					.asInstanceOf(InstanceOfAssertFactories.list(TaskDecorator.class))
					.containsExactly(context.getBean("taskDecorator1", TaskDecorator.class),
							context.getBean("taskDecorator3", TaskDecorator.class),
							context.getBean("taskDecorator2", TaskDecorator.class));
			});
	}

	@Test
	void simpleAsyncTaskExecutorBuilderUsesPlatformThreadsByDefault() {
		this.contextRunner.run((context) -> {
			SimpleAsyncTaskExecutorBuilder builder = context.getBean(SimpleAsyncTaskExecutorBuilder.class);
			assertThat(builder).hasFieldOrPropertyWithValue("virtualThreads", null);
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void simpleAsyncTaskExecutorBuilderUsesVirtualThreadsWhenEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true").run((context) -> {
			SimpleAsyncTaskExecutorBuilder builder = context.getBean(SimpleAsyncTaskExecutorBuilder.class);
			assertThat(builder).hasFieldOrPropertyWithValue("virtualThreads", true);
		});
	}

	@Test
	void taskExecutorWhenHasCustomTaskExecutorShouldBackOff() {
		this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new).run((context) -> {
			assertThat(context).hasSingleBean(Executor.class);
			assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
		});
	}

	@Test
	void taskExecutorWhenModeIsAutoAndHasCustomTaskExecutorShouldBackOff() {
		this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new)
			.withPropertyValues("spring.task.execution.mode=auto")
			.run((context) -> {
				assertThat(context).hasSingleBean(Executor.class);
				assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
			});
	}

	@Test
	void taskExecutorWhenModeIsForceAndHasCustomTaskExecutorShouldCreateApplicationTaskExecutor() {
		this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new)
			.withPropertyValues("spring.task.execution.mode=force")
			.run((context) -> assertThat(context.getBeansOfType(Executor.class)).hasSize(2)
				.containsKeys("customTaskExecutor", "applicationTaskExecutor"));
	}

	@Test
	void taskExecutorWhenModeIsForceAndHasCustomTaskExecutorWithReservedNameShouldThrowException() {
		this.contextRunner.withBean("applicationTaskExecutor", Executor.class, SyncTaskExecutor::new)
			.withPropertyValues("spring.task.execution.mode=force")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.isInstanceOf(BeanDefinitionOverrideException.class));
	}

	@Test
	void taskExecutorWhenModeIsForceAndHasCustomBFPPCanRestoreTaskExecutorAlias() {
		this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new)
			.withPropertyValues("spring.task.execution.mode=force")
			.withBean(BeanFactoryPostProcessor.class,
					() -> (beanFactory) -> beanFactory.registerAlias("applicationTaskExecutor", "taskExecutor"))
			.run((context) -> {
				assertThat(context.getBeansOfType(Executor.class)).hasSize(2)
					.containsKeys("customTaskExecutor", "applicationTaskExecutor");
				assertThat(context).hasBean("taskExecutor");
				assertThat(context.getBean("taskExecutor")).isSameAs(context.getBean("applicationTaskExecutor"));
			});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void whenVirtualThreadsAreEnabledAndCustomTaskExecutorIsDefinedThenSimpleAsyncTaskExecutorThatUsesVirtualThreadsBacksOff() {
		this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new)
			.withPropertyValues("spring.threads.virtual.enabled=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(Executor.class);
				assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
			});
	}

	@Test
	void enableAsyncUsesAutoConfiguredOneByDefault() {
		this.contextRunner.withPropertyValues("spring.task.execution.thread-name-prefix=auto-task-")
			.withUserConfiguration(AsyncConfiguration.class, TestBean.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AsyncConfigurer.class);
				assertThat(context).hasSingleBean(TaskExecutor.class);
				TestBean bean = context.getBean(TestBean.class);
				String text = bean.echo("something").get();
				assertThat(text).contains("auto-task-").contains("something");
			});
	}

	@Test
	void enableAsyncUsesCustomExecutorIfPresent() {
		this.contextRunner.withPropertyValues("spring.task.execution.thread-name-prefix=auto-task-")
			.withBean("customTaskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"))
			.withUserConfiguration(AsyncConfiguration.class, TestBean.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(AsyncConfigurer.class);
				assertThat(context).hasSingleBean(Executor.class);
				TestBean bean = context.getBean(TestBean.class);
				String text = bean.echo("something").get();
				assertThat(text).contains("custom-task-").contains("something");
			});
	}

	@Test
	void enableAsyncUsesAutoConfiguredExecutorWhenModeIsForceAndHasCustomTaskExecutor() {
		this.contextRunner
			.withPropertyValues("spring.task.execution.thread-name-prefix=auto-task-",
					"spring.task.execution.mode=force")
			.withBean("customTaskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"))
			.withUserConfiguration(AsyncConfiguration.class, TestBean.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AsyncConfigurer.class);
				assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
				TestBean bean = context.getBean(TestBean.class);
				String text = bean.echo("something").get();
				assertThat(text).contains("auto-task-").contains("something");
			});
	}

	@Test
	void enableAsyncUsesAutoConfiguredExecutorWhenModeIsForceAndHasCustomTaskExecutorWithReservedName() {
		this.contextRunner
			.withPropertyValues("spring.task.execution.thread-name-prefix=auto-task-",
					"spring.task.execution.mode=force")
			.withBean("taskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"))
			.withUserConfiguration(AsyncConfiguration.class, TestBean.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AsyncConfigurer.class);
				assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
				TestBean bean = context.getBean(TestBean.class);
				String text = bean.echo("something").get();
				assertThat(text).contains("auto-task-").contains("something");
			});
	}

	@Test
	void enableAsyncUsesAsyncConfigurerWhenModeIsForce() {
		this.contextRunner
			.withPropertyValues("spring.task.execution.thread-name-prefix=auto-task-",
					"spring.task.execution.mode=force")
			.withBean("taskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"))
			.withBean("customAsyncConfigurer", AsyncConfigurer.class, () -> new AsyncConfigurer() {
				@Override
				public Executor getAsyncExecutor() {
					return createCustomAsyncExecutor("async-task-");
				}
			})
			.withUserConfiguration(AsyncConfiguration.class, TestBean.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AsyncConfigurer.class);
				assertThat(context.getBeansOfType(Executor.class)).hasSize(2)
					.containsOnlyKeys("taskExecutor", "applicationTaskExecutor");
				TestBean bean = context.getBean(TestBean.class);
				String text = bean.echo("something").get();
				assertThat(text).contains("async-task-").contains("something");
			});
	}

	@Test
	void enableAsyncUsesAutoConfiguredExecutorWhenModeIsForceAndHasPrimaryCustomTaskExecutor() {
		this.contextRunner
			.withPropertyValues("spring.task.execution.thread-name-prefix=auto-task-",
					"spring.task.execution.mode=force")
			.withBean("taskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"),
					(beanDefinition) -> beanDefinition.setPrimary(true))
			.withUserConfiguration(AsyncConfiguration.class, TestBean.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AsyncConfigurer.class);
				assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
				TestBean bean = context.getBean(TestBean.class);
				String text = bean.echo("something").get();
				assertThat(text).contains("auto-task-").contains("something");
			});
	}

	@Test
	void enableAsyncUsesAutoConfiguredOneByDefaultEvenThoughSchedulingIsConfigured() {
		this.contextRunner.withPropertyValues("spring.task.execution.thread-name-prefix=auto-task-")
			.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
			.withUserConfiguration(AsyncConfiguration.class, SchedulingConfiguration.class, TestBean.class)
			.run((context) -> {
				TestBean bean = context.getBean(TestBean.class);
				String text = bean.echo("something").get();
				assertThat(text).contains("auto-task-").contains("something");
			});
	}

	@Test
	void shouldAliasApplicationTaskExecutorToBootstrapExecutor() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Executor.class)
				.hasBean("applicationTaskExecutor")
				.hasBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
			assertThat(context.getAliases("applicationTaskExecutor"))
				.containsExactly(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
			assertThat(context.getBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME))
				.isSameAs(context.getBean("applicationTaskExecutor"));
		});
	}

	@Test
	void shouldNotAliasApplicationTaskExecutorWhenBootstrapExecutorIsDefined() {
		this.contextRunner.withBean("applicationTaskExecutor", Executor.class, () -> createCustomAsyncExecutor("app-"))
			.withBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME, Executor.class,
					() -> createCustomAsyncExecutor("bootstrap-"))
			.run((context) -> {
				assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
				assertThat(context).hasBean("applicationTaskExecutor")
					.hasBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
				assertThat(context.getAliases("applicationTaskExecutor")).isEmpty();
				assertThat(context.getBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME))
					.isNotSameAs(context.getBean("applicationTaskExecutor"));
			});
	}

	@Test
	void shouldNotAliasApplicationTaskExecutorWhenApplicationTaskExecutorIsMissing() {
		this.contextRunner.withBean("customExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-"))
			.run((context) -> assertThat(context).hasSingleBean(Executor.class)
				.hasBean("customExecutor")
				.doesNotHaveBean("applicationTaskExecutor")
				.doesNotHaveBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME));
	}

	@Test
	void shouldNotAliasApplicationTaskExecutorWhenBootstrapExecutorRegisteredAsSingleton() {
		this.contextRunner.withBean("applicationTaskExecutor", Executor.class, () -> createCustomAsyncExecutor("app-"))
			.withInitializer((context) -> context.getBeanFactory()
				.registerSingleton(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME,
						createCustomAsyncExecutor("bootstrap-")))
			.run((context) -> {
				assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
				assertThat(context).hasBean("applicationTaskExecutor")
					.hasBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
				assertThat(context.getAliases("applicationTaskExecutor")).isEmpty();
				assertThat(context.getBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME))
					.isNotSameAs(context.getBean("applicationTaskExecutor"));
			});
	}

	@Test
	void shouldNotAliasApplicationTaskExecutorWhenBootstrapExecutorAliasIsDefined() {
		Executor executor = Runnable::run;
		this.contextRunner.withBean("applicationTaskExecutor", Executor.class, () -> executor)
			.withBean("customExecutor", Executor.class, () -> createCustomAsyncExecutor("custom"))
			.withInitializer((context) -> context.getBeanFactory()
				.registerAlias("customExecutor", ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME))
			.run((context) -> {
				assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
				assertThat(context).hasBean("applicationTaskExecutor").hasBean("customExecutor");
				assertThat(context.getAliases("applicationTaskExecutor")).isEmpty();
				assertThat(context.getAliases("customExecutor"))
					.contains(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
				assertThat(context.getBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME))
					.isNotSameAs(context.getBean("applicationTaskExecutor"))
					.isSameAs(context.getBean("customExecutor"));
			});
	}

	private Executor createCustomAsyncExecutor(String threadNamePrefix) {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix(threadNamePrefix);
		return executor;
	}

	private ContextConsumer<AssertableApplicationContext> assertThreadPoolTaskExecutor(
			Consumer<ThreadPoolTaskExecutor> taskExecutor) {
		return (context) -> {
			assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
			ThreadPoolTaskExecutorBuilder builder = context.getBean(ThreadPoolTaskExecutorBuilder.class);
			taskExecutor.accept(builder.build());
		};
	}

	private ContextConsumer<AssertableApplicationContext> assertSimpleAsyncTaskExecutor(
			Consumer<SimpleAsyncTaskExecutor> taskExecutor) {
		return (context) -> {
			assertThat(context).hasSingleBean(SimpleAsyncTaskExecutorBuilder.class);
			SimpleAsyncTaskExecutorBuilder builder = context.getBean(SimpleAsyncTaskExecutorBuilder.class);
			taskExecutor.accept(builder.build());
		};
	}

	private String virtualThreadName(SimpleAsyncTaskExecutor taskExecutor) throws InterruptedException {
		AtomicReference<Thread> threadReference = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);
		taskExecutor.execute(() -> {
			Thread currentThread = Thread.currentThread();
			threadReference.set(currentThread);
			latch.countDown();
		});
		assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
		Thread thread = threadReference.get();
		assertThat(thread).extracting("virtual").as("%s is virtual", thread).isEqualTo(true);
		return thread.getName();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomThreadPoolTaskExecutorBuilderConfig {

		private final ThreadPoolTaskExecutorBuilder builder = new ThreadPoolTaskExecutorBuilder();

		@Bean
		ThreadPoolTaskExecutorBuilder customThreadPoolTaskExecutorBuilder() {
			return this.builder;
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
			return CompletableFuture.completedFuture(Thread.currentThread().getName() + " " + text);
		}

	}

}
