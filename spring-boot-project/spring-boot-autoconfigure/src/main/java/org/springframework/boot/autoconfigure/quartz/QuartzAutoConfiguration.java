/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.quartz;

import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Quartz Scheduler.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
		LiquibaseAutoConfiguration.class, FlywayAutoConfiguration.class })
@ConditionalOnClass({ Scheduler.class, SchedulerFactoryBean.class, PlatformTransactionManager.class })
@EnableConfigurationProperties(QuartzProperties.class)
public class QuartzAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SchedulerFactoryBean quartzScheduler(QuartzProperties properties,
			ObjectProvider<SchedulerFactoryBeanCustomizer> customizers, ObjectProvider<JobDetail> jobDetails,
			Map<String, Calendar> calendars, ObjectProvider<Trigger> triggers, ApplicationContext applicationContext) {
		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
		SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		schedulerFactoryBean.setJobFactory(jobFactory);
		if (properties.getSchedulerName() != null) {
			schedulerFactoryBean.setSchedulerName(properties.getSchedulerName());
		}
		schedulerFactoryBean.setAutoStartup(properties.isAutoStartup());
		schedulerFactoryBean.setStartupDelay((int) properties.getStartupDelay().getSeconds());
		schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(properties.isWaitForJobsToCompleteOnShutdown());
		schedulerFactoryBean.setOverwriteExistingJobs(properties.isOverwriteExistingJobs());
		if (!properties.getProperties().isEmpty()) {
			schedulerFactoryBean.setQuartzProperties(asProperties(properties.getProperties()));
		}
		schedulerFactoryBean.setJobDetails(jobDetails.orderedStream().toArray(JobDetail[]::new));
		schedulerFactoryBean.setCalendars(calendars);
		schedulerFactoryBean.setTriggers(triggers.orderedStream().toArray(Trigger[]::new));
		customizers.orderedStream().forEach((customizer) -> customizer.customize(schedulerFactoryBean));
		return schedulerFactoryBean;
	}

	private Properties asProperties(Map<String, String> source) {
		Properties properties = new Properties();
		properties.putAll(source);
		return properties;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(DataSource.class)
	@ConditionalOnProperty(prefix = "spring.quartz", name = "job-store-type", havingValue = "jdbc")
	@Import(DatabaseInitializationDependencyConfigurer.class)
	protected static class JdbcStoreTypeConfiguration {

		@Bean
		@Order(0)
		public SchedulerFactoryBeanCustomizer dataSourceCustomizer(QuartzProperties properties, DataSource dataSource,
				@QuartzDataSource ObjectProvider<DataSource> quartzDataSource,
				ObjectProvider<PlatformTransactionManager> transactionManager,
				@QuartzTransactionManager ObjectProvider<PlatformTransactionManager> quartzTransactionManager) {
			return (schedulerFactoryBean) -> {
				DataSource dataSourceToUse = getDataSource(dataSource, quartzDataSource);
				schedulerFactoryBean.setDataSource(dataSourceToUse);
				PlatformTransactionManager txManager = getTransactionManager(transactionManager,
						quartzTransactionManager);
				if (txManager != null) {
					schedulerFactoryBean.setTransactionManager(txManager);
				}
			};
		}

		private DataSource getDataSource(DataSource dataSource, ObjectProvider<DataSource> quartzDataSource) {
			DataSource dataSourceIfAvailable = quartzDataSource.getIfAvailable();
			return (dataSourceIfAvailable != null) ? dataSourceIfAvailable : dataSource;
		}

		private PlatformTransactionManager getTransactionManager(
				ObjectProvider<PlatformTransactionManager> transactionManager,
				ObjectProvider<PlatformTransactionManager> quartzTransactionManager) {
			PlatformTransactionManager transactionManagerIfAvailable = quartzTransactionManager.getIfAvailable();
			return (transactionManagerIfAvailable != null) ? transactionManagerIfAvailable
					: transactionManager.getIfUnique();
		}

		@Bean
		@ConditionalOnMissingBean(QuartzDataSourceScriptDatabaseInitializer.class)
		@Conditional(OnQuartzDatasourceInitializationCondition.class)
		public QuartzDataSourceScriptDatabaseInitializer quartzDataSourceScriptDatabaseInitializer(
				DataSource dataSource, @QuartzDataSource ObjectProvider<DataSource> quartzDataSource,
				QuartzProperties properties) {
			DataSource dataSourceToUse = getDataSource(dataSource, quartzDataSource);
			return new QuartzDataSourceScriptDatabaseInitializer(dataSourceToUse, properties);
		}

		static class OnQuartzDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

			OnQuartzDatasourceInitializationCondition() {
				super("Quartz", "spring.quartz.jdbc.initialize-schema");
			}

		}

	}

}
