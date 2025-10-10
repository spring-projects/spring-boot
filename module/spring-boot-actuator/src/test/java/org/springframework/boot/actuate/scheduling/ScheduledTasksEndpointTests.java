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

package org.springframework.boot.actuate.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.CronTaskDescriptor;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.CustomTriggerTaskDescriptor;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.FixedDelayTaskDescriptor;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.FixedRateTaskDescriptor;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.LastExecution;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.NextExecution;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.ScheduledTasksDescriptor;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.ScheduledTasksEndpointRuntimeHints;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.TaskDescriptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TaskExecutionOutcome.Status;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduledTasksEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Brian Clozel
 */
class ScheduledTasksEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(BaseConfiguration.class);

	@Test
	void cronScheduledMethodIsReported() {
		run(CronScheduledMethod.class, (tasks) -> {
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getCron()).hasSize(1);
			CronTaskDescriptor description = (CronTaskDescriptor) tasks.getCron().get(0);
			assertThat(description.getExpression()).isEqualTo("0 0 0/3 1/1 * ?");
			assertThat(description.getRunnable().getTarget()).isEqualTo(CronScheduledMethod.class.getName() + ".cron");
			NextExecution nextExecution = description.getNextExecution();
			assertThat(nextExecution).isNotNull();
			assertThat(nextExecution.getTime()).isInTheFuture();
			assertThat(description.getLastExecution()).isNull();
		});
	}

	@Test
	void cronTriggerIsReported() {
		run(CronTriggerTask.class, (tasks) -> {
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getCron()).hasSize(1);
			CronTaskDescriptor description = (CronTaskDescriptor) tasks.getCron().get(0);
			assertThat(description.getExpression()).isEqualTo("0 0 0/6 1/1 * ?");
			assertThat(description.getRunnable().getTarget()).contains(CronTriggerRunnable.class.getName());
			assertThat(description.getLastExecution()).isNull();
		});
	}

	@Test
	void fixedDelayScheduledMethodIsReported() {
		run(FixedDelayScheduledMethod.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedDelay()).hasSize(1);
			FixedDelayTaskDescriptor description = (FixedDelayTaskDescriptor) tasks.getFixedDelay().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(2000);
			assertThat(description.getInterval()).isEqualTo(1000);
			assertThat(description.getRunnable().getTarget())
				.isEqualTo(FixedDelayScheduledMethod.class.getName() + ".fixedDelay");
			assertThat(description.getLastExecution()).isNull();
		});
	}

	@Test
	void fixedDelayTriggerIsReported() {
		run(FixedDelayTriggerTask.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedDelay()).hasSize(1);
			FixedDelayTaskDescriptor description = (FixedDelayTaskDescriptor) tasks.getFixedDelay().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(2000);
			assertThat(description.getInterval()).isEqualTo(1000);
			assertThat(description.getRunnable().getTarget()).contains(FixedDelayTriggerRunnable.class.getName());
			assertThat(description.getLastExecution()).isNull();
		});
	}

	@Test
	void noInitialDelayFixedDelayTriggerIsReported() {
		run(NoInitialDelayFixedDelayTriggerTask.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedDelay()).hasSize(1);
			FixedDelayTaskDescriptor description = (FixedDelayTaskDescriptor) tasks.getFixedDelay().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(0);
			assertThat(description.getInterval()).isEqualTo(1000);
			assertThat(description.getRunnable().getTarget()).contains(FixedDelayTriggerRunnable.class.getName());
			assertThatTaskMayHaveBeenExecuted(description);
		});
	}

	@Test
	void fixedRateScheduledMethodIsReported() {
		run(FixedRateScheduledMethod.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedRate()).hasSize(1);
			FixedRateTaskDescriptor description = (FixedRateTaskDescriptor) tasks.getFixedRate().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(4000);
			assertThat(description.getInterval()).isEqualTo(3000);
			assertThat(description.getRunnable().getTarget())
				.isEqualTo(FixedRateScheduledMethod.class.getName() + ".fixedRate");
			assertThat(description.getLastExecution()).isNull();
		});
	}

	@Test
	void fixedRateTriggerIsReported() {
		run(FixedRateTriggerTask.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedRate()).hasSize(1);
			FixedRateTaskDescriptor description = (FixedRateTaskDescriptor) tasks.getFixedRate().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(3000);
			assertThat(description.getInterval()).isEqualTo(2000);
			assertThat(description.getRunnable().getTarget()).contains(FixedRateTriggerRunnable.class.getName());
			assertThat(description.getLastExecution()).isNull();
		});
	}

	@Test
	void noInitialDelayFixedRateTriggerIsReported() {
		run(NoInitialDelayFixedRateTriggerTask.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedRate()).hasSize(1);
			FixedRateTaskDescriptor description = (FixedRateTaskDescriptor) tasks.getFixedRate().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(0);
			assertThat(description.getInterval()).isEqualTo(2000);
			assertThat(description.getRunnable().getTarget()).contains(FixedRateTriggerRunnable.class.getName());
			assertThatTaskMayHaveBeenExecuted(description);
		});
	}

	@Test
	void taskWithCustomTriggerIsReported() {
		run(CustomTriggerTask.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getCustom()).hasSize(1);
			CustomTriggerTaskDescriptor description = (CustomTriggerTaskDescriptor) tasks.getCustom().get(0);
			assertThat(description.getRunnable().getTarget()).contains(CustomTriggerRunnable.class.getName());
			assertThat(description.getTrigger()).isEqualTo(CustomTriggerTask.trigger.toString());
			assertThatTaskMayHaveBeenExecuted(description);
		});
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new ScheduledTasksEndpointRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		Set<Class<?>> bindingTypes = Set.of(FixedRateTaskDescriptor.class, FixedDelayTaskDescriptor.class,
				CronTaskDescriptor.class, CustomTriggerTaskDescriptor.class);
		for (Class<?> bindingType : bindingTypes) {
			assertThat(RuntimeHintsPredicates.reflection()
				.onType(bindingType)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
		}
	}

	private void assertThatTaskMayHaveBeenExecuted(TaskDescriptor descriptor) {
		LastExecution lastExecution = descriptor.getLastExecution();
		if (lastExecution != null) {
			if (lastExecution.getStatus() == Status.SUCCESS) {
				assertThat(lastExecution.getTime()).isInThePast();
				assertThat(lastExecution.getException()).isNull();
			}
		}
	}

	private void run(Class<?> configuration, Consumer<ScheduledTasksDescriptor> consumer) {
		this.contextRunner.withUserConfiguration(configuration)
			.run((context) -> consumer.accept(context.getBean(ScheduledTasksEndpoint.class).scheduledTasks()));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	static class BaseConfiguration {

		@Bean
		ScheduledTasksEndpoint endpoint(Collection<ScheduledTaskHolder> scheduledTaskHolders) {
			return new ScheduledTasksEndpoint(scheduledTaskHolders);
		}

	}

	static class FixedDelayScheduledMethod {

		@Scheduled(fixedDelay = 1000, initialDelay = 2000)
		void fixedDelay() {

		}

	}

	static class FixedRateScheduledMethod {

		@Scheduled(fixedRate = 3000, initialDelay = 4000)
		void fixedRate() {

		}

	}

	static class CronScheduledMethod {

		@Scheduled(cron = "0 0 0/3 1/1 * ?")
		void cron() {

		}

	}

	static class FixedDelayTriggerTask implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(1));
			trigger.setInitialDelay(Duration.ofSeconds(2));
			taskRegistrar.addTriggerTask(new FixedDelayTriggerRunnable(), trigger);
		}

	}

	static class NoInitialDelayFixedDelayTriggerTask implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(1));
			taskRegistrar.addTriggerTask(new FixedDelayTriggerRunnable(), trigger);
		}

	}

	static class FixedRateTriggerTask implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(2));
			trigger.setInitialDelay(Duration.ofSeconds(3));
			trigger.setFixedRate(true);
			taskRegistrar.addTriggerTask(new FixedRateTriggerRunnable(), trigger);
		}

	}

	static class NoInitialDelayFixedRateTriggerTask implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(2));
			trigger.setFixedRate(true);
			taskRegistrar.addTriggerTask(new FixedRateTriggerRunnable(), trigger);
		}

	}

	static class CronTriggerTask implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.addTriggerTask(new CronTriggerRunnable(), new CronTrigger("0 0 0/6 1/1 * ?"));
		}

	}

	static class CustomTriggerTask implements SchedulingConfigurer {

		private static final Trigger trigger = (context) -> Instant.now();

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.addTriggerTask(new CustomTriggerRunnable(), trigger);
		}

	}

	static class CronTriggerRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

	static class FixedDelayTriggerRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

	static class FixedRateTriggerRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

	static class CustomTriggerRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

}
