/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.batch;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Batch. If a single job is
 * found in the context, it will be executed on startup.
 * <p>
 * Disable this behavior with {@literal spring.batch.job.enabled=false}).
 * <p>
 * If multiple jobs are found, a job name to execute on startup can be supplied by the
 * User with : {@literal spring.batch.job.name=job1}. In this case the Runner will first
 * find jobs registered as Beans, then those in the existing JobRegistry.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Kazuki Shimizu
 * @author Mahmoud Ben Hassine
 * @author Lars Uffmann
 * @author Lasse Wulff
 * @since 1.0.0
 */
@AutoConfiguration(after = { HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class })
@ConditionalOnClass({ JobLauncher.class, DataSource.class, DatabasePopulator.class })
@ConditionalOnBean({ DataSource.class, PlatformTransactionManager.class })
@ConditionalOnMissingBean(value = DefaultBatchConfiguration.class, annotation = EnableBatchProcessing.class)
@EnableConfigurationProperties(BatchProperties.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
public class BatchAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBooleanProperty(name = "spring.batch.job.enabled", matchIfMissing = true)
	public JobLauncherApplicationRunner jobLauncherApplicationRunner(JobLauncher jobLauncher, JobExplorer jobExplorer,
			JobRepository jobRepository, BatchProperties properties) {
		JobLauncherApplicationRunner runner = new JobLauncherApplicationRunner(jobLauncher, jobExplorer, jobRepository);
		String jobName = properties.getJob().getName();
		if (StringUtils.hasText(jobName)) {
			runner.setJobName(jobName);
		}
		return runner;
	}

	@Bean
	@ConditionalOnMissingBean(ExitCodeGenerator.class)
	public JobExecutionExitCodeGenerator jobExecutionExitCodeGenerator() {
		return new JobExecutionExitCodeGenerator();
	}

	@Configuration(proxyBeanMethods = false)
	static class SpringBootBatchConfiguration extends DefaultBatchConfiguration {

		private final DataSource dataSource;

		private final PlatformTransactionManager transactionManager;

		private final TaskExecutor taskExector;

		private final BatchProperties properties;

		private final List<BatchConversionServiceCustomizer> batchConversionServiceCustomizers;

		private final ExecutionContextSerializer executionContextSerializer;

		SpringBootBatchConfiguration(DataSource dataSource, @BatchDataSource ObjectProvider<DataSource> batchDataSource,
				PlatformTransactionManager transactionManager,
				@BatchTransactionManager ObjectProvider<PlatformTransactionManager> batchTransactionManager,
				@BatchTaskExecutor ObjectProvider<TaskExecutor> batchTaskExecutor, BatchProperties properties,
				ObjectProvider<BatchConversionServiceCustomizer> batchConversionServiceCustomizers,
				ObjectProvider<ExecutionContextSerializer> executionContextSerializer) {
			this.dataSource = batchDataSource.getIfAvailable(() -> dataSource);
			this.transactionManager = batchTransactionManager.getIfAvailable(() -> transactionManager);
			this.taskExector = batchTaskExecutor.getIfAvailable();
			this.properties = properties;
			this.batchConversionServiceCustomizers = batchConversionServiceCustomizers.orderedStream().toList();
			this.executionContextSerializer = executionContextSerializer.getIfAvailable();
		}

		@Override
		protected DataSource getDataSource() {
			return this.dataSource;
		}

		@Override
		protected PlatformTransactionManager getTransactionManager() {
			return this.transactionManager;
		}

		@Override
		protected String getTablePrefix() {
			String tablePrefix = this.properties.getJdbc().getTablePrefix();
			return (tablePrefix != null) ? tablePrefix : super.getTablePrefix();
		}

		@Override
		protected Isolation getIsolationLevelForCreate() {
			Isolation isolation = this.properties.getJdbc().getIsolationLevelForCreate();
			return (isolation != null) ? isolation : super.getIsolationLevelForCreate();
		}

		@Override
		protected ConfigurableConversionService getConversionService() {
			ConfigurableConversionService conversionService = super.getConversionService();
			for (BatchConversionServiceCustomizer customizer : this.batchConversionServiceCustomizers) {
				customizer.customize(conversionService);
			}
			return conversionService;
		}

		@Override
		protected ExecutionContextSerializer getExecutionContextSerializer() {
			return (this.executionContextSerializer != null) ? this.executionContextSerializer
					: super.getExecutionContextSerializer();
		}

		@Override
		protected TaskExecutor getTaskExecutor() {
			return (this.taskExector != null) ? this.taskExector : super.getTaskExecutor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnBatchDatasourceInitializationCondition.class)
	static class DataSourceInitializerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(DataSource dataSource,
				@BatchDataSource ObjectProvider<DataSource> batchDataSource, BatchProperties properties) {
			return new BatchDataSourceScriptDatabaseInitializer(batchDataSource.getIfAvailable(() -> dataSource),
					properties.getJdbc());
		}

	}

	static class OnBatchDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		OnBatchDatasourceInitializationCondition() {
			super("Batch", "spring.batch.jdbc.initialize-schema", "spring.batch.initialize-schema");
		}

	}

}
