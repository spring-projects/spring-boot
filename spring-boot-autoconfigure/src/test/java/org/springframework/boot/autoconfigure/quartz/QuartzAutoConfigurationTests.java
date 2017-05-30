/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.quartz;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.quartz.Calendar;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.calendar.MonthlyCalendar;
import org.quartz.impl.calendar.WeeklyCalendar;
import org.quartz.simpl.RAMJobStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.scheduling.quartz.LocalTaskExecutorThreadPool;
import org.springframework.scheduling.quartz.QuartzJobBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Tests for {@link QuartzAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
public class QuartzAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCapture output = new OutputCapture();

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void withDatabase() throws Exception {
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				QuartzAutoConfiguration.class);
		Scheduler scheduler = this.context.getBean(Scheduler.class);

		assertThat(scheduler).isNotNull();
		assertThat(scheduler.getMetaData().getJobStoreClass())
				.isAssignableFrom(LocalDataSourceJobStore.class);
	}

	@Test
	public void withNoDatabase() throws Exception {
		registerAndRefresh(QuartzAutoConfiguration.class);
		Scheduler scheduler = this.context.getBean(Scheduler.class);

		assertThat(scheduler).isNotNull();
		assertThat(scheduler.getMetaData().getJobStoreClass())
				.isAssignableFrom(RAMJobStore.class);
	}

	@Test
	public void withTaskExecutor() throws Exception {
		registerAndRefresh(QuartzAutoConfiguration.class,
				QuartzExecutorConfiguration.class);
		Scheduler scheduler = this.context.getBean(Scheduler.class);

		assertThat(scheduler).isNotNull();
		assertThat(scheduler.getMetaData().getThreadPoolClass())
				.isEqualTo(LocalTaskExecutorThreadPool.class);
	}

	@Test
	public void withConfiguredJobAndTrigger() throws Exception {
		TestPropertyValues.of("test-name=withConfiguredJobAndTrigger")
				.applyTo(this.context);
		registerAndRefresh(QuartzAutoConfiguration.class, QuartzJobConfiguration.class);
		Scheduler scheduler = this.context.getBean(Scheduler.class);

		assertThat(scheduler.getJobDetail(JobKey.jobKey("fooJob"))).isNotNull();
		assertThat(scheduler.getTrigger(TriggerKey.triggerKey("fooTrigger"))).isNotNull();
		Thread.sleep(1000L);
		this.output.expect(containsString("withConfiguredJobAndTrigger"));
	}

	@Test
	public void withConfiguredCalendars() throws Exception {
		registerAndRefresh(QuartzAutoConfiguration.class,
				QuartzCalendarsConfiguration.class);
		Scheduler scheduler = this.context.getBean(Scheduler.class);

		assertThat(scheduler.getCalendar("weekly")).isNotNull();
		assertThat(scheduler.getCalendar("monthly")).isNotNull();
	}

	@Test
	public void withQuartzProperties() throws Exception {
		TestPropertyValues
				.of("spring.quartz.properties.org.quartz.scheduler.instanceId=FOO")
				.applyTo(this.context);
		registerAndRefresh(QuartzAutoConfiguration.class);
		Scheduler scheduler = this.context.getBean(Scheduler.class);

		assertThat(scheduler).isNotNull();
		assertThat(scheduler.getSchedulerInstanceId()).isEqualTo("FOO");
	}

	@Test
	public void withCustomizer() throws Exception {
		registerAndRefresh(QuartzAutoConfiguration.class, QuartzCustomConfig.class);
		Scheduler scheduler = this.context.getBean(Scheduler.class);

		assertThat(scheduler).isNotNull();
		assertThat(scheduler.getSchedulerName()).isEqualTo("fooScheduler");
	}

	private void registerAndRefresh(Class<?>... annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
	}

	@Configuration
	protected static class QuartzJobConfiguration {

		@Bean
		public JobDetail fooJob() {
			return JobBuilder.newJob().ofType(FooJob.class).withIdentity("fooJob")
					.storeDurably().build();
		}

		@Bean
		public Trigger fooTrigger() {
			SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
					.withIntervalInSeconds(10).repeatForever();

			return TriggerBuilder.newTrigger().forJob(fooJob()).withIdentity("fooTrigger")
					.withSchedule(scheduleBuilder).build();
		}

	}

	@Configuration
	protected static class QuartzCalendarsConfiguration {

		@Bean
		public Calendar weekly() {
			return new WeeklyCalendar();
		}

		@Bean
		public Calendar monthly() {
			return new MonthlyCalendar();
		}

	}

	@Configuration
	protected static class QuartzExecutorConfiguration {

		@Bean
		public Executor executor() {
			return Executors.newSingleThreadExecutor();
		}

	}

	@Configuration
	protected static class QuartzCustomConfig {

		@Bean
		public SchedulerFactoryBeanCustomizer customizer() {
			return schedulerFactoryBean -> schedulerFactoryBean
					.setSchedulerName("fooScheduler");
		}

	}

	public static class FooJob extends QuartzJobBean {

		@Autowired
		private Environment env;

		@Override
		protected void executeInternal(JobExecutionContext context)
				throws JobExecutionException {
			System.out.println(this.env.getProperty("test-name", "unknown"));
		}

	}

}
