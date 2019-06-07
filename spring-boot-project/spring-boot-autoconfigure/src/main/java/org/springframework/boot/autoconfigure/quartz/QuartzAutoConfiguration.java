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

package org.springframework.boot.autoconfigure.quartz;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Quartz Scheduler.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ Scheduler.class, SchedulerFactoryBean.class, PlatformTransactionManager.class })
@EnableConfigurationProperties(QuartzProperties.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
public class QuartzAutoConfiguration {

	private final QuartzProperties properties;

	private final List<SchedulerFactoryBeanCustomizer> customizers;

	private final JobDetail[] jobDetails;

	private final Map<String, Calendar> calendars;

	private final Trigger[] triggers;

	private final ApplicationContext applicationContext;

	public QuartzAutoConfiguration(QuartzProperties properties,
			ObjectProvider<List<SchedulerFactoryBeanCustomizer>> customizers, ObjectProvider<JobDetail[]> jobDetails,
			ObjectProvider<Map<String, Calendar>> calendars, ObjectProvider<Trigger[]> triggers,
			ApplicationContext applicationContext) {
		this.properties = properties;
		this.customizers = customizers.getIfAvailable();
		this.jobDetails = jobDetails.getIfAvailable();
		this.calendars = calendars.getIfAvailable();
		this.triggers = triggers.getIfAvailable();
		this.applicationContext = applicationContext;
	}

	@Bean
	@ConditionalOnMissingBean
	public SchedulerFactoryBean quartzScheduler() {
		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
		schedulerFactoryBean.setJobFactory(
				new AutowireCapableBeanJobFactory(this.applicationContext.getAutowireCapableBeanFactory()));
		if (!this.properties.getProperties().isEmpty()) {
			schedulerFactoryBean.setQuartzProperties(asProperties(this.properties.getProperties()));
		}
		if (this.jobDetails != null && this.jobDetails.length > 0) {
			schedulerFactoryBean.setJobDetails(this.jobDetails);
		}
		if (this.calendars != null && !this.calendars.isEmpty()) {
			schedulerFactoryBean.setCalendars(this.calendars);
		}
		if (this.triggers != null && this.triggers.length > 0) {
			schedulerFactoryBean.setTriggers(this.triggers);
		}
		customize(schedulerFactoryBean);
		return schedulerFactoryBean;
	}

	private Properties asProperties(Map<String, String> source) {
		Properties properties = new Properties();
		properties.putAll(source);
		return properties;
	}

	private void customize(SchedulerFactoryBean schedulerFactoryBean) {
		if (this.customizers != null) {
			for (SchedulerFactoryBeanCustomizer customizer : this.customizers) {
				customizer.customize(schedulerFactoryBean);
			}
		}
	}

	@Configuration
	@ConditionalOnSingleCandidate(DataSource.class)
	protected static class JdbcStoreTypeConfiguration {

		@Bean
		@Order(0)
		public SchedulerFactoryBeanCustomizer dataSourceCustomizer(QuartzProperties properties, DataSource dataSource,
				@QuartzDataSource ObjectProvider<DataSource> quartzDataSource,
				ObjectProvider<PlatformTransactionManager> transactionManager) {
			return (schedulerFactoryBean) -> {
				if (properties.getJobStoreType() == JobStoreType.JDBC) {
					DataSource dataSourceToUse = getDataSource(dataSource, quartzDataSource);
					schedulerFactoryBean.setDataSource(dataSourceToUse);
					PlatformTransactionManager txManager = transactionManager.getIfUnique();
					if (txManager != null) {
						schedulerFactoryBean.setTransactionManager(txManager);
					}
				}
			};
		}

		private DataSource getDataSource(DataSource dataSource, ObjectProvider<DataSource> quartzDataSource) {
			DataSource dataSourceIfAvailable = quartzDataSource.getIfAvailable();
			return (dataSourceIfAvailable != null) ? dataSourceIfAvailable : dataSource;
		}

		@Bean
		@ConditionalOnMissingBean
		public QuartzDataSourceInitializer quartzDataSourceInitializer(DataSource dataSource,
				@QuartzDataSource ObjectProvider<DataSource> quartzDataSource, ResourceLoader resourceLoader,
				QuartzProperties properties) {
			DataSource dataSourceToUse = getDataSource(dataSource, quartzDataSource);
			return new QuartzDataSourceInitializer(dataSourceToUse, resourceLoader, properties);
		}

		@Bean
		public static DataSourceInitializerSchedulerDependencyPostProcessor dataSourceInitializerSchedulerDependencyPostProcessor() {
			return new DataSourceInitializerSchedulerDependencyPostProcessor();
		}

		private static class DataSourceInitializerSchedulerDependencyPostProcessor
				extends AbstractDependsOnBeanFactoryPostProcessor {

			DataSourceInitializerSchedulerDependencyPostProcessor() {
				super(Scheduler.class, SchedulerFactoryBean.class, "quartzDataSourceInitializer");
			}

		}

	}

}
