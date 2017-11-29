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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.Calendar;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.calendar.MonthlyCalendar;
import org.quartz.impl.calendar.WeeklyCalendar;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.scheduling.quartz.LocalTaskExecutorThreadPool;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Tests for {@link QuartzAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class QuartzAutoConfigurationTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void withNoDataSource() throws Exception {
		load();
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getMetaData().getJobStoreClass())
				.isAssignableFrom(RAMJobStore.class);
	}

	@Test
	public void withDataSourceUseMemoryByDefault() throws Exception {
		load(new Class<?>[] { EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class });
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getMetaData().getJobStoreClass())
				.isAssignableFrom(RAMJobStore.class);
	}

	@Test
	public void withDataSource() throws Exception {
		load(new Class<?>[] { QuartzJobsConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class },
				"spring.quartz.job-store-type=jdbc");
		testWithDataSource();
	}

	@Test
	public void withDataSourceNoTransactionManager() throws Exception {
		load(new Class<?>[] { QuartzJobsConfiguration.class,
				EmbeddedDataSourceConfiguration.class },
				"spring.quartz.job-store-type=jdbc");
		testWithDataSource();
	}

	private void testWithDataSource() throws SchedulerException {
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getMetaData().getJobStoreClass())
				.isAssignableFrom(LocalDataSourceJobStore.class);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(
				this.context.getBean(DataSource.class));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM QRTZ_JOB_DETAILS",
				Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM QRTZ_SIMPLE_TRIGGERS", Integer.class)).isEqualTo(0);
	}

	@Test
	public void withTaskExecutor() throws Exception {
		load(QuartzExecutorConfiguration.class);
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getMetaData().getThreadPoolClass())
				.isEqualTo(LocalTaskExecutorThreadPool.class);
	}

	@Test
	public void withMultipleTaskExecutors() throws Exception {
		load(QuartzMultipleExecutorsConfiguration.class);
		assertThat(this.context.getBeansOfType(Executor.class)).hasSize(2);
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getMetaData().getThreadPoolClass())
				.isEqualTo(SimpleThreadPool.class);
	}

	@Test
	public void withMultipleTaskExecutorsWithPrimary() throws Exception {
		load(QuartzMultipleExecutorsWithPrimaryConfiguration.class);
		assertThat(this.context.getBeansOfType(Executor.class)).hasSize(2);
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getMetaData().getThreadPoolClass())
				.isEqualTo(LocalTaskExecutorThreadPool.class);
	}

	@Test
	public void withMultipleTaskExecutorsWithCustomizer() throws Exception {
		load(QuartzMultipleExecutorsWithCustomizerConfiguration.class);
		assertThat(this.context.getBeansOfType(Executor.class)).hasSize(3);
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getMetaData().getThreadPoolClass())
				.isEqualTo(LocalTaskExecutorThreadPool.class);
	}

	@Test
	public void withConfiguredJobAndTrigger() throws Exception {
		load(QuartzFullConfiguration.class, "test-name=withConfiguredJobAndTrigger");
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getJobDetail(JobKey.jobKey("fooJob"))).isNotNull();
		assertThat(scheduler.getTrigger(TriggerKey.triggerKey("fooTrigger"))).isNotNull();
		Thread.sleep(1000L);
		this.output.expect(containsString("withConfiguredJobAndTrigger"));
		this.output.expect(containsString("jobDataValue"));
	}

	@Test
	public void withConfiguredCalendars() throws Exception {
		load(QuartzCalendarsConfiguration.class);
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getCalendar("weekly")).isNotNull();
		assertThat(scheduler.getCalendar("monthly")).isNotNull();
	}

	@Test
	public void withQuartzProperties() throws Exception {
		load("spring.quartz.properties.org.quartz.scheduler.instanceId=FOO");
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getSchedulerInstanceId()).isEqualTo("FOO");
	}

	@Test
	public void withCustomizer() throws Exception {
		load(QuartzCustomConfiguration.class);
		assertThat(this.context.getBeansOfType(Scheduler.class)).hasSize(1);
		Scheduler scheduler = this.context.getBean(Scheduler.class);
		assertThat(scheduler.getSchedulerName()).isEqualTo("fooScheduler");
	}

	private void load(String... environment) {
		load(new Class<?>[0], environment);
	}

	private void load(Class<?> config, String... environment) {
		load(new Class<?>[] { config }, environment);
	}

	private void load(Class<?>[] configs, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		if (!ObjectUtils.isEmpty(configs)) {
			ctx.register(configs);
		}
		ctx.register(QuartzAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Import(ComponentThatUsesScheduler.class)
	@Configuration
	protected static class BaseQuartzConfiguration {

	}

	@Configuration
	protected static class QuartzJobsConfiguration extends BaseQuartzConfiguration {

		@Bean
		public JobDetail fooJob() {
			return JobBuilder.newJob().ofType(FooJob.class).withIdentity("fooJob")
					.storeDurably().build();
		}

		@Bean
		public JobDetail barJob() {
			return JobBuilder.newJob().ofType(FooJob.class).withIdentity("barJob")
					.storeDurably().build();
		}

	}

	@Configuration
	protected static class QuartzFullConfiguration extends BaseQuartzConfiguration {

		@Bean
		public JobDetail fooJob() {
			return JobBuilder.newJob().ofType(FooJob.class).withIdentity("fooJob")
					.usingJobData("jobDataKey", "jobDataValue").storeDurably().build();
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
	protected static class QuartzCalendarsConfiguration extends BaseQuartzConfiguration {

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
	protected static class QuartzExecutorConfiguration extends BaseQuartzConfiguration {

		@Bean
		public Executor executor() {
			return Executors.newSingleThreadExecutor();
		}

	}

	@Configuration
	protected static class QuartzMultipleExecutorsConfiguration
			extends QuartzExecutorConfiguration {

		@Bean
		public Executor anotherExecutor() {
			return Executors.newSingleThreadExecutor();
		}

	}

	@Configuration
	protected static class QuartzMultipleExecutorsWithPrimaryConfiguration
			extends QuartzExecutorConfiguration {

		@Bean
		@Primary
		public Executor primaryExecutor() {
			return Executors.newSingleThreadExecutor();
		}

	}

	@Configuration
	protected static class QuartzMultipleExecutorsWithCustomizerConfiguration
			extends QuartzMultipleExecutorsConfiguration {

		@Bean
		public Executor yetAnotherExecutor() {
			return Executors.newSingleThreadExecutor();
		}

		@Bean
		public SchedulerFactoryBeanCustomizer customizer() {
			return (schedulerFactoryBean) -> schedulerFactoryBean
					.setTaskExecutor(yetAnotherExecutor());
		}

	}

	@Configuration
	protected static class QuartzCustomConfiguration extends BaseQuartzConfiguration {

		@Bean
		public SchedulerFactoryBeanCustomizer customizer() {
			return (schedulerFactoryBean) -> schedulerFactoryBean
					.setSchedulerName("fooScheduler");
		}

	}

	public static class ComponentThatUsesScheduler {

		public ComponentThatUsesScheduler(Scheduler scheduler) {
			Assert.notNull(scheduler, "Scheduler must not be null");
		}

	}

	public static class FooJob extends QuartzJobBean {

		@Autowired
		private Environment env;

		private String jobDataKey;

		@Override
		protected void executeInternal(JobExecutionContext context)
				throws JobExecutionException {
			System.out.println(this.env.getProperty("test-name", "unknown") + " - "
					+ this.jobDataKey);
		}

		public void setJobDataKey(String jobDataKey) {
			this.jobDataKey = jobDataKey;
		}

	}

}
