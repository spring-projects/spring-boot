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

package org.springframework.boot.quartz.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.Calendar;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.calendar.MonthlyCalendar;
import org.quartz.impl.calendar.WeeklyCalendar;
import org.quartz.simpl.RAMJobStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link QuartzAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class QuartzAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.generate-unique-name=true")
		.withConfiguration(AutoConfigurations.of(QuartzAutoConfiguration.class));

	@Test
	void withNoDataSource() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Scheduler.class);
			Scheduler scheduler = context.getBean(Scheduler.class);
			assertThat(scheduler.getMetaData().getJobStoreClass()).isAssignableFrom(RAMJobStore.class);
		});
	}

	@Test
	void withDataSourceUseMemoryByDefault() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(Scheduler.class);
				Scheduler scheduler = context.getBean(Scheduler.class);
				assertThat(scheduler.getMetaData().getJobStoreClass()).isAssignableFrom(RAMJobStore.class);
			});
	}

	@Test
	void withDataSource() {
		this.contextRunner.withUserConfiguration(QuartzJobsConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.withPropertyValues("spring.quartz.job-store-type=jdbc")
			.run(assertDataSourceInitializedByDataSourceDatabaseScriptInitializer("dataSource"));
	}

	@Test
	void withDataSourceAndInMemoryStoreDoesNotInitializeDataSource() {
		this.contextRunner.withUserConfiguration(QuartzJobsConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.withPropertyValues("spring.quartz.job-store-type=memory")
			.run((context) -> {
				JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean("dataSource", DataSource.class));
				assertThat(jdbcTemplate.queryForList("SHOW TABLES")
					.stream()
					.map((table) -> (String) table.get("TABLE_NAME"))).noneMatch((name) -> name.startsWith("QRTZ"));
			});
	}

	@Test
	void dataSourceInitializationBacksOffWithoutSpringBootJdbc() {
		this.contextRunner.withBean(DataSource.class, QuartzAutoConfigurationTests::createTestDataSource)
			.withBean(SchedulerFactoryBeanCustomizer.class, () -> (schedulerFactoryBean) -> {
				// Mock out the scheduler so that the context doesn't fail to start
				// due to missing tables that the JDBC job store requires.
				try {
					SchedulerFactory factory = mock(SchedulerFactory.class);
					given(factory.getScheduler()).willReturn(mock(Scheduler.class));
					given(factory.getScheduler(anyString())).willReturn(mock(Scheduler.class));
					schedulerFactoryBean.setSchedulerFactory(factory);
				}
				catch (SchedulerException ex) {
					throw new RuntimeException(ex);
				}
			})
			.withClassLoader(new FilteredClassLoader("org.springframework.boot.jdbc", "org.springframework.boot.sql") {

				@Override
				public Enumeration<URL> getResources(String name) throws IOException {
					Enumeration<URL> resources = super.getResources(name);
					if (!name.equals("META-INF/spring.factories")) {
						return resources;
					}
					// Hide META-INF/spring.factories files with entries from
					// org.springframework.boot.jdbc
					return Collections.enumeration(Collections.list(resources).stream().filter((url) -> {
						try (InputStream input = url.openStream()) {
							String content = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
							return !content.contains("org.springframework.boot.jdbc.");
						}
						catch (IOException ex) {
							return true;
						}
					}).toList());
				}

			})
			.withPropertyValues("spring.quartz.job-store-type=jdbc")
			.run((context) -> assertThat(context).doesNotHaveBean(QuartzDataSourceScriptDatabaseInitializer.class));
	}

	@Test
	void withDataSourceNoTransactionManager() {
		this.contextRunner.withUserConfiguration(QuartzJobsConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.quartz.job-store-type=jdbc")
			.run(assertDataSourceInitializedByDataSourceDatabaseScriptInitializer("dataSource"));
	}

	@Test
	void dataSourceWithQuartzDataSourceQualifierUsedWhenMultiplePresent() {
		this.contextRunner.withUserConfiguration(QuartzJobsConfiguration.class, MultipleDataSourceConfiguration.class)
			.withPropertyValues("spring.quartz.job-store-type=jdbc")
			.run(assertDataSourceInitializedByDataSourceDatabaseScriptInitializer("quartzDataSource"));
	}

	@Test
	void transactionManagerWithQuartzTransactionManagerUsedWhenMultiplePresent() {
		this.contextRunner
			.withUserConfiguration(QuartzJobsConfiguration.class, MultipleTransactionManagersConfiguration.class)
			.withPropertyValues("spring.quartz.job-store-type=jdbc")
			.run((context) -> {
				SchedulerFactoryBean schedulerFactoryBean = context.getBean(SchedulerFactoryBean.class);
				assertThat(schedulerFactoryBean).extracting("transactionManager")
					.isEqualTo(context.getBean("quartzTransactionManager"));
			});
	}

	@Test
	void withTaskExecutor() {
		this.contextRunner.withUserConfiguration(MockExecutorConfiguration.class)
			.withPropertyValues("spring.quartz.properties.org.quartz.threadPool.threadCount=50")
			.run((context) -> {
				assertThat(context).hasSingleBean(Scheduler.class);
				Scheduler scheduler = context.getBean(Scheduler.class);
				assertThat(scheduler.getMetaData().getThreadPoolSize()).isEqualTo(50);
				Executor executor = context.getBean(Executor.class);
				then(executor).shouldHaveNoInteractions();
			});
	}

	@Test
	void withOverwriteExistingJobs() {
		this.contextRunner.withUserConfiguration(OverwriteTriggerConfiguration.class)
			.withPropertyValues("spring.quartz.overwrite-existing-jobs=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(Scheduler.class);
				Scheduler scheduler = context.getBean(Scheduler.class);
				Trigger fooTrigger = scheduler.getTrigger(TriggerKey.triggerKey("fooTrigger"));
				assertThat(fooTrigger).isNotNull();
				assertThat(((SimpleTrigger) fooTrigger).getRepeatInterval()).isEqualTo(30000);
			});
	}

	@Test
	void withConfiguredJobAndTrigger(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(QuartzFullConfiguration.class)
			.withPropertyValues("test-name=withConfiguredJobAndTrigger")
			.run((context) -> {
				assertThat(context).hasSingleBean(Scheduler.class);
				Scheduler scheduler = context.getBean(Scheduler.class);
				assertThat(scheduler.getJobDetail(JobKey.jobKey("fooJob"))).isNotNull();
				assertThat(scheduler.getTrigger(TriggerKey.triggerKey("fooTrigger"))).isNotNull();
				Awaitility.waitAtMost(Duration.ofSeconds(5))
					.untilAsserted(
							() -> assertThat(output).contains("withConfiguredJobAndTrigger").contains("jobDataValue"));
			});
	}

	@Test
	void withConfiguredCalendars() {
		this.contextRunner.withUserConfiguration(QuartzCalendarsConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Scheduler.class);
			Scheduler scheduler = context.getBean(Scheduler.class);
			assertThat(scheduler.getCalendar("weekly")).isNotNull();
			assertThat(scheduler.getCalendar("monthly")).isNotNull();
		});
	}

	@Test
	void withQuartzProperties() {
		this.contextRunner.withPropertyValues("spring.quartz.properties.org.quartz.scheduler.instanceId=FOO")
			.run((context) -> {
				assertThat(context).hasSingleBean(Scheduler.class);
				Scheduler scheduler = context.getBean(Scheduler.class);
				assertThat(scheduler.getSchedulerInstanceId()).isEqualTo("FOO");
			});
	}

	@Test
	void withCustomizer() {
		this.contextRunner.withUserConfiguration(QuartzCustomConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Scheduler.class);
			Scheduler scheduler = context.getBean(Scheduler.class);
			assertThat(scheduler.getSchedulerName()).isEqualTo("fooScheduler");
		});
	}

	@Test
	void validateDefaultProperties() {
		this.contextRunner.withUserConfiguration(ManualSchedulerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(SchedulerFactoryBean.class);
			SchedulerFactoryBean schedulerFactory = context.getBean(SchedulerFactoryBean.class);
			QuartzProperties properties = new QuartzProperties();
			assertThat(properties.isAutoStartup()).isEqualTo(schedulerFactory.isAutoStartup());
			assertThat(schedulerFactory).hasFieldOrPropertyWithValue("startupDelay",
					(int) properties.getStartupDelay().getSeconds());
			assertThat(schedulerFactory).hasFieldOrPropertyWithValue("waitForJobsToCompleteOnShutdown",
					properties.isWaitForJobsToCompleteOnShutdown());
			assertThat(schedulerFactory).hasFieldOrPropertyWithValue("overwriteExistingJobs",
					properties.isOverwriteExistingJobs());

		});

	}

	@Test
	void withCustomConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.quartz.auto-startup=false", "spring.quartz.startup-delay=1m",
					"spring.quartz.wait-for-jobs-to-complete-on-shutdown=true",
					"spring.quartz.overwrite-existing-jobs=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(SchedulerFactoryBean.class);
				SchedulerFactoryBean schedulerFactory = context.getBean(SchedulerFactoryBean.class);
				assertThat(schedulerFactory.isAutoStartup()).isFalse();
				assertThat(schedulerFactory).hasFieldOrPropertyWithValue("startupDelay", 60);
				assertThat(schedulerFactory).hasFieldOrPropertyWithValue("waitForJobsToCompleteOnShutdown", true);
				assertThat(schedulerFactory).hasFieldOrPropertyWithValue("overwriteExistingJobs", true);
			});
	}

	@Test
	void withLiquibase() {
		this.contextRunner.withUserConfiguration(QuartzJobsConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class, LiquibaseAutoConfiguration.class))
			.withPropertyValues("spring.quartz.job-store-type=jdbc", "spring.quartz.jdbc.initialize-schema=never",
					"spring.liquibase.change-log=classpath:org/quartz/impl/jdbcjobstore/liquibase.quartz.init.xml")
			.run(assertDataSourceInitialized("dataSource").andThen(
					(context) -> assertThat(context).doesNotHaveBean(QuartzDataSourceScriptDatabaseInitializer.class)));
	}

	@Test
	void withFlyway(@TempDir Path flywayLocation) throws Exception {
		ClassPathResource tablesResource = new ClassPathResource("org/quartz/impl/jdbcjobstore/tables_h2.sql");
		try (InputStream stream = tablesResource.getInputStream()) {
			Files.copy(stream, flywayLocation.resolve("V2__quartz.sql"));
		}
		this.contextRunner.withUserConfiguration(QuartzJobsConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class, FlywayAutoConfiguration.class))
			.withPropertyValues("spring.quartz.job-store-type=jdbc", "spring.quartz.jdbc.initialize-schema=never",
					"spring.flyway.locations=filesystem:" + flywayLocation, "spring.flyway.baseline-on-migrate=true")
			.run(assertDataSourceInitialized("dataSource").andThen(
					(context) -> assertThat(context).doesNotHaveBean(QuartzDataSourceScriptDatabaseInitializer.class)));
	}

	@Test
	void schedulerNameWithDedicatedProperty() {
		this.contextRunner.withPropertyValues("spring.quartz.scheduler-name=testScheduler")
			.run(assertSchedulerName("testScheduler"));
	}

	@Test
	void schedulerNameWithQuartzProperty() {
		this.contextRunner
			.withPropertyValues("spring.quartz.properties.org.quartz.scheduler.instanceName=testScheduler")
			.run(assertSchedulerName("testScheduler"));
	}

	@Test
	void schedulerNameWithDedicatedPropertyTakesPrecedence() {
		this.contextRunner
			.withPropertyValues("spring.quartz.scheduler-name=specificTestScheduler",
					"spring.quartz.properties.org.quartz.scheduler.instanceName=testScheduler")
			.run(assertSchedulerName("specificTestScheduler"));
	}

	@Test
	void schedulerNameUseBeanNameByDefault() {
		this.contextRunner.withPropertyValues().run(assertSchedulerName("quartzScheduler"));
	}

	@Test
	void whenTheUserDefinesTheirOwnQuartzDatabaseInitializerThenTheAutoConfiguredInitializerBacksOff() {
		this.contextRunner.withUserConfiguration(CustomQuartzDatabaseInitializerConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.withPropertyValues("spring.quartz.job-store-type=jdbc")
			.run((context) -> assertThat(context).hasSingleBean(QuartzDataSourceScriptDatabaseInitializer.class)
				.doesNotHaveBean("quartzDataSourceScriptDatabaseInitializer")
				.hasBean("customInitializer"));
	}

	@Test
	void whenTheUserDefinesTheirOwnDatabaseInitializerThenTheAutoConfiguredQuartzInitializerRemains() {
		this.contextRunner.withUserConfiguration(CustomDatabaseInitializerConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.withPropertyValues("spring.quartz.job-store-type=jdbc")
			.run((context) -> assertThat(context).hasSingleBean(QuartzDataSourceScriptDatabaseInitializer.class)
				.hasBean("customInitializer"));
	}

	private ContextConsumer<AssertableApplicationContext> assertDataSourceInitialized(String dataSourceName) {
		return (context) -> {
			assertThat(context).hasSingleBean(Scheduler.class);
			Scheduler scheduler = context.getBean(Scheduler.class);
			assertThat(scheduler.getMetaData().getJobStoreClass()).isAssignableFrom(LocalDataSourceJobStore.class);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(dataSourceName, DataSource.class));
			assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM QRTZ_JOB_DETAILS", Integer.class))
				.isEqualTo(2);
			assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM QRTZ_SIMPLE_TRIGGERS", Integer.class))
				.isZero();
		};
	}

	private ContextConsumer<AssertableApplicationContext> assertDataSourceInitializedByDataSourceDatabaseScriptInitializer(
			String dataSourceName) {
		return assertDataSourceInitialized(dataSourceName).andThen((context) -> {
			assertThat(context).hasSingleBean(QuartzDataSourceScriptDatabaseInitializer.class);
			QuartzDataSourceScriptDatabaseInitializer initializer = context
				.getBean(QuartzDataSourceScriptDatabaseInitializer.class);
			assertThat(initializer).hasFieldOrPropertyWithValue("dataSource", context.getBean(dataSourceName));
		});
	}

	private ContextConsumer<AssertableApplicationContext> assertSchedulerName(String schedulerName) {
		return (context) -> {
			assertThat(context).hasSingleBean(SchedulerFactoryBean.class);
			SchedulerFactoryBean schedulerFactory = context.getBean(SchedulerFactoryBean.class);
			assertThat(schedulerFactory).hasFieldOrPropertyWithValue("schedulerName", schedulerName);
		};
	}

	private static DataSource createTestDataSource() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setGenerateUniqueName(true);
		try {
			properties.afterPropertiesSet();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return properties.initializeDataSourceBuilder().build();
	}

	@Import(ComponentThatUsesScheduler.class)
	@Configuration(proxyBeanMethods = false)
	static class BaseQuartzConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class QuartzJobsConfiguration extends BaseQuartzConfiguration {

		@Bean
		JobDetail fooJob() {
			return JobBuilder.newJob().ofType(FooJob.class).withIdentity("fooJob").storeDurably().build();
		}

		@Bean
		JobDetail barJob() {
			return JobBuilder.newJob().ofType(FooJob.class).withIdentity("barJob").storeDurably().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class QuartzFullConfiguration extends BaseQuartzConfiguration {

		@Bean
		JobDetail fooJob() {
			return JobBuilder.newJob()
				.ofType(FooJob.class)
				.withIdentity("fooJob")
				.usingJobData("jobDataKey", "jobDataValue")
				.storeDurably()
				.build();
		}

		@Bean
		Trigger fooTrigger(JobDetail jobDetail) {
			SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
				.withIntervalInSeconds(10)
				.repeatForever();

			return TriggerBuilder.newTrigger()
				.forJob(jobDetail)
				.withIdentity("fooTrigger")
				.withSchedule(scheduleBuilder)
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(QuartzFullConfiguration.class)
	static class OverwriteTriggerConfiguration extends BaseQuartzConfiguration {

		@Bean
		Trigger anotherFooTrigger(JobDetail fooJob) {
			SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
				.withIntervalInSeconds(30)
				.repeatForever();

			return TriggerBuilder.newTrigger()
				.forJob(fooJob)
				.withIdentity("fooTrigger")
				.withSchedule(scheduleBuilder)
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class QuartzCalendarsConfiguration extends BaseQuartzConfiguration {

		@Bean
		Calendar weekly() {
			return new WeeklyCalendar();
		}

		@Bean
		Calendar monthly() {
			return new MonthlyCalendar();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MockExecutorConfiguration extends BaseQuartzConfiguration {

		@Bean
		Executor executor() {
			return mock(Executor.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class QuartzCustomConfiguration extends BaseQuartzConfiguration {

		@Bean
		SchedulerFactoryBeanCustomizer customizer() {
			return (schedulerFactoryBean) -> schedulerFactoryBean.setSchedulerName("fooScheduler");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ManualSchedulerConfiguration {

		@Bean
		SchedulerFactoryBean quartzScheduler() {
			return new SchedulerFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleDataSourceConfiguration extends BaseQuartzConfiguration {

		@Bean
		@Primary
		DataSource applicationDataSource() {
			return createTestDataSource();
		}

		@QuartzDataSource
		@Bean
		DataSource quartzDataSource() {
			return createTestDataSource();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleTransactionManagersConfiguration extends BaseQuartzConfiguration {

		private final DataSource primaryDataSource = createTestDataSource();

		private final DataSource quartzDataSource = createTestDataSource();

		@Bean
		@Primary
		DataSource applicationDataSource() {
			return this.primaryDataSource;
		}

		@Bean
		@QuartzDataSource
		DataSource quartzDataSource() {
			return this.quartzDataSource;
		}

		@Bean
		@Primary
		PlatformTransactionManager applicationTransactionManager() {
			return new DataSourceTransactionManager(this.primaryDataSource);
		}

		@Bean
		@QuartzTransactionManager
		PlatformTransactionManager quartzTransactionManager() {
			return new DataSourceTransactionManager(this.quartzDataSource);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomQuartzDatabaseInitializerConfiguration {

		@Bean
		QuartzDataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource,
				QuartzJdbcProperties properties) {
			return new QuartzDataSourceScriptDatabaseInitializer(dataSource, properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDatabaseInitializerConfiguration {

		@Bean
		DataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource) {
			return new DataSourceScriptDatabaseInitializer(dataSource, new DatabaseInitializationSettings());
		}

	}

	static class ComponentThatUsesScheduler {

		ComponentThatUsesScheduler(Scheduler scheduler) {
			Assert.notNull(scheduler, "'scheduler' must not be null");
		}

	}

	public static class FooJob extends QuartzJobBean {

		@Autowired
		private Environment env;

		private String jobDataKey;

		@Override
		protected void executeInternal(JobExecutionContext context) {
			System.out.println(this.env.getProperty("test-name", "unknown") + " - " + this.jobDataKey);
		}

		public void setJobDataKey(String jobDataKey) {
			this.jobDataKey = jobDataKey;
		}

	}

}
