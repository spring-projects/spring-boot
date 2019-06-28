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

package org.springframework.boot.actuate.scheduling;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.CronTaskDescription;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.CustomTriggerTaskDescription;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.FixedDelayTaskDescription;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.FixedRateTaskDescription;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.ScheduledTasksReport;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduledTasksEndpoint}.
 *
 * @author Andy Wilkinson
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
			CronTaskDescription description = (CronTaskDescription) tasks.getCron().get(0);
			assertThat(description.getExpression()).isEqualTo("0 0 0/3 1/1 * ?");
			assertThat(description.getRunnable().getTarget()).isEqualTo(CronScheduledMethod.class.getName() + ".cron");
		});
	}

	@Test
	void cronTriggerIsReported() {
		run(CronTriggerTask.class, (tasks) -> {
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getCron()).hasSize(1);
			CronTaskDescription description = (CronTaskDescription) tasks.getCron().get(0);
			assertThat(description.getExpression()).isEqualTo("0 0 0/6 1/1 * ?");
			assertThat(description.getRunnable().getTarget()).isEqualTo(CronTriggerRunnable.class.getName());
		});
	}

	@Test
	void fixedDelayScheduledMethodIsReported() {
		run(FixedDelayScheduledMethod.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedDelay()).hasSize(1);
			FixedDelayTaskDescription description = (FixedDelayTaskDescription) tasks.getFixedDelay().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(2);
			assertThat(description.getInterval()).isEqualTo(1);
			assertThat(description.getRunnable().getTarget())
					.isEqualTo(FixedDelayScheduledMethod.class.getName() + ".fixedDelay");
		});
	}

	@Test
	void fixedDelayTriggerIsReported() {
		run(FixedDelayTriggerTask.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedDelay()).hasSize(1);
			FixedDelayTaskDescription description = (FixedDelayTaskDescription) tasks.getFixedDelay().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(2000);
			assertThat(description.getInterval()).isEqualTo(1000);
			assertThat(description.getRunnable().getTarget()).isEqualTo(FixedDelayTriggerRunnable.class.getName());
		});
	}

	@Test
	void fixedRateScheduledMethodIsReported() {
		run(FixedRateScheduledMethod.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedRate()).hasSize(1);
			FixedRateTaskDescription description = (FixedRateTaskDescription) tasks.getFixedRate().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(4);
			assertThat(description.getInterval()).isEqualTo(3);
			assertThat(description.getRunnable().getTarget())
					.isEqualTo(FixedRateScheduledMethod.class.getName() + ".fixedRate");
		});
	}

	@Test
	void fixedRateTriggerIsReported() {
		run(FixedRateTriggerTask.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getCustom()).isEmpty();
			assertThat(tasks.getFixedRate()).hasSize(1);
			FixedRateTaskDescription description = (FixedRateTaskDescription) tasks.getFixedRate().get(0);
			assertThat(description.getInitialDelay()).isEqualTo(3000);
			assertThat(description.getInterval()).isEqualTo(2000);
			assertThat(description.getRunnable().getTarget()).isEqualTo(FixedRateTriggerRunnable.class.getName());
		});
	}

	@Test
	void taskWithCustomTriggerIsReported() {
		run(CustomTriggerTask.class, (tasks) -> {
			assertThat(tasks.getCron()).isEmpty();
			assertThat(tasks.getFixedDelay()).isEmpty();
			assertThat(tasks.getFixedRate()).isEmpty();
			assertThat(tasks.getCustom()).hasSize(1);
			CustomTriggerTaskDescription description = (CustomTriggerTaskDescription) tasks.getCustom().get(0);
			assertThat(description.getRunnable().getTarget()).isEqualTo(CustomTriggerRunnable.class.getName());
			assertThat(description.getTrigger()).isEqualTo(CustomTriggerTask.trigger.toString());
		});
	}

	private void run(Class<?> configuration, Consumer<ScheduledTasksReport> consumer) {
		this.contextRunner.withUserConfiguration(configuration)
				.run((context) -> consumer.accept(context.getBean(ScheduledTasksEndpoint.class).scheduledTasks()));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	static class BaseConfiguration {

		@Bean
		public ScheduledTasksEndpoint endpoint(Collection<ScheduledTaskHolder> scheduledTaskHolders) {
			return new ScheduledTasksEndpoint(scheduledTaskHolders);
		}

	}

	private static class FixedDelayScheduledMethod {

		@Scheduled(fixedDelay = 1, initialDelay = 2)
		public void fixedDelay() {

		}

	}

	private static class FixedRateScheduledMethod {

		@Scheduled(fixedRate = 3, initialDelay = 4)
		public void fixedRate() {

		}

	}

	private static class CronScheduledMethod {

		@Scheduled(cron = "0 0 0/3 1/1 * ?")
		public void cron() {

		}

	}

	private static class FixedDelayTriggerTask implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			PeriodicTrigger trigger = new PeriodicTrigger(1, TimeUnit.SECONDS);
			trigger.setInitialDelay(2);
			taskRegistrar.addTriggerTask(new FixedDelayTriggerRunnable(), trigger);
		}

	}

	private static class FixedRateTriggerTask implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			PeriodicTrigger trigger = new PeriodicTrigger(2, TimeUnit.SECONDS);
			trigger.setInitialDelay(3);
			trigger.setFixedRate(true);
			taskRegistrar.addTriggerTask(new FixedRateTriggerRunnable(), trigger);
		}

	}

	private static class CronTriggerTask implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.addTriggerTask(new CronTriggerRunnable(), new CronTrigger("0 0 0/6 1/1 * ?"));
		}

	}

	private static class CustomTriggerTask implements SchedulingConfigurer {

		private static final Trigger trigger = (context) -> new Date();

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.addTriggerTask(new CustomTriggerRunnable(), trigger);
		}

	}

	private static class CronTriggerRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

	private static class FixedDelayTriggerRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

	private static class FixedRateTriggerRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

	private static class CustomTriggerRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

}
