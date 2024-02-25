/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * @since 1.0.0
 */
@AutoConfiguration(after = { HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class })
@ConditionalOnClass({ JobLauncher.class, DataSource.class, DatabasePopulator.class })
@ConditionalOnBean({ DataSource.class, PlatformTransactionManager.class })
@ConditionalOnMissingBean(value = DefaultBatchConfiguration.class, annotation = EnableBatchProcessing.class)
@EnableConfigurationProperties(BatchProperties.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
public class BatchAutoConfiguration {

	/**
     * Creates a {@link JobLauncherApplicationRunner} bean if it is missing and the property "spring.batch.job.enabled" is set to true.
     * 
     * @param jobLauncher - The {@link JobLauncher} bean.
     * @param jobExplorer - The {@link JobExplorer} bean.
     * @param jobRepository - The {@link JobRepository} bean.
     * @param properties - The {@link BatchProperties} bean.
     * @return The created {@link JobLauncherApplicationRunner} bean.
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job", name = "enabled", havingValue = "true", matchIfMissing = true)
	public JobLauncherApplicationRunner jobLauncherApplicationRunner(JobLauncher jobLauncher, JobExplorer jobExplorer,
			JobRepository jobRepository, BatchProperties properties) {
		JobLauncherApplicationRunner runner = new JobLauncherApplicationRunner(jobLauncher, jobExplorer, jobRepository);
		String jobName = properties.getJob().getName();
		if (StringUtils.hasText(jobName)) {
			runner.setJobName(jobName);
		}
		return runner;
	}

	/**
     * Generates a JobExecutionExitCodeGenerator bean if no other bean of type ExitCodeGenerator is present.
     * 
     * @return the JobExecutionExitCodeGenerator bean
     */
    @Bean
	@ConditionalOnMissingBean(ExitCodeGenerator.class)
	public JobExecutionExitCodeGenerator jobExecutionExitCodeGenerator() {
		return new JobExecutionExitCodeGenerator();
	}

	/**
     * SpringBootBatchConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	static class SpringBootBatchConfiguration extends DefaultBatchConfiguration {

		private final DataSource dataSource;

		private final PlatformTransactionManager transactionManager;

		private final BatchProperties properties;

		private final List<BatchConversionServiceCustomizer> batchConversionServiceCustomizers;

		private final ExecutionContextSerializer executionContextSerializer;

		/**
         * Constructs a new SpringBootBatchConfiguration with the specified parameters.
         *
         * @param dataSource The primary data source to be used for the batch processing.
         * @param batchDataSource The optional data source specifically for batch processing.
         * @param transactionManager The transaction manager to be used for the batch processing.
         * @param properties The properties for the batch processing.
         * @param batchConversionServiceCustomizers The customizers for the batch conversion service.
         * @param executionContextSerializer The serializer for the execution context.
         */
        SpringBootBatchConfiguration(DataSource dataSource, @BatchDataSource ObjectProvider<DataSource> batchDataSource,
				PlatformTransactionManager transactionManager, BatchProperties properties,
				ObjectProvider<BatchConversionServiceCustomizer> batchConversionServiceCustomizers,
				ObjectProvider<ExecutionContextSerializer> executionContextSerializer) {
			this.dataSource = batchDataSource.getIfAvailable(() -> dataSource);
			this.transactionManager = transactionManager;
			this.properties = properties;
			this.batchConversionServiceCustomizers = batchConversionServiceCustomizers.orderedStream().toList();
			this.executionContextSerializer = executionContextSerializer
				.getIfAvailable(DefaultExecutionContextSerializer::new);
		}

		/**
         * Returns the data source used by this SpringBootBatchConfiguration.
         *
         * @return the data source used by this configuration
         */
        @Override
		protected DataSource getDataSource() {
			return this.dataSource;
		}

		/**
         * Returns the transaction manager used for managing transactions in the Spring Boot Batch Configuration.
         *
         * @return the transaction manager
         */
        @Override
		protected PlatformTransactionManager getTransactionManager() {
			return this.transactionManager;
		}

		/**
         * Retrieves the table prefix from the properties and returns it.
         * If the table prefix is not set, it returns the default table prefix.
         *
         * @return The table prefix to be used for database tables.
         */
        @Override
		protected String getTablePrefix() {
			String tablePrefix = this.properties.getJdbc().getTablePrefix();
			return (tablePrefix != null) ? tablePrefix : super.getTablePrefix();
		}

		/**
         * Retrieves the isolation level for creating a new transaction.
         * 
         * @return The isolation level for creating a new transaction.
         */
        @Override
		protected Isolation getIsolationLevelForCreate() {
			Isolation isolation = this.properties.getJdbc().getIsolationLevelForCreate();
			return (isolation != null) ? isolation : super.getIsolationLevelForCreate();
		}

		/**
         * Returns the configurable conversion service.
         * 
         * This method overrides the getConversionService() method from the parent class and adds customizations to the conversion service.
         * 
         * @return The configurable conversion service with customizations applied.
         */
        @Override
		protected ConfigurableConversionService getConversionService() {
			ConfigurableConversionService conversionService = super.getConversionService();
			for (BatchConversionServiceCustomizer customizer : this.batchConversionServiceCustomizers) {
				customizer.customize(conversionService);
			}
			return conversionService;
		}

		/**
         * Returns the execution context serializer used by this SpringBootBatchConfiguration.
         *
         * @return the execution context serializer
         */
        @Override
		protected ExecutionContextSerializer getExecutionContextSerializer() {
			return this.executionContextSerializer;
		}

	}

	/**
     * DataSourceInitializerConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@Conditional(OnBatchDatasourceInitializationCondition.class)
	static class DataSourceInitializerConfiguration {

		/**
         * Creates a {@link BatchDataSourceScriptDatabaseInitializer} bean if no bean of type {@link BatchDataSourceScriptDatabaseInitializer} is already present.
         * 
         * @param dataSource The primary {@link DataSource} bean.
         * @param batchDataSource An {@link ObjectProvider} for the {@link DataSource} bean annotated with {@link BatchDataSource}.
         * @param properties The {@link BatchProperties} bean.
         * @return A new instance of {@link BatchDataSourceScriptDatabaseInitializer}.
         */
        @Bean
		@ConditionalOnMissingBean(BatchDataSourceScriptDatabaseInitializer.class)
		BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(DataSource dataSource,
				@BatchDataSource ObjectProvider<DataSource> batchDataSource, BatchProperties properties) {
			return new BatchDataSourceScriptDatabaseInitializer(batchDataSource.getIfAvailable(() -> dataSource),
					properties.getJdbc());
		}

	}

	/**
     * OnBatchDatasourceInitializationCondition class.
     */
    static class OnBatchDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		/**
         * Constructor for OnBatchDatasourceInitializationCondition.
         * 
         * @param name
         *            the name of the condition
         * @param systemProperty
         *            the system property to check for
         * @param environmentProperty
         *            the environment property to check for
         */
        OnBatchDatasourceInitializationCondition() {
			super("Batch", "spring.batch.jdbc.initialize-schema", "spring.batch.initialize-schema");
		}

	}

}
