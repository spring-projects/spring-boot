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

package org.springframework.boot.batch.autoconfigure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.converter.JsonJobParametersConverter;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration.SpringBootBatchConfiguration;
import org.springframework.boot.batch.autoconfigure.domain.City;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BatchAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Kazuki Shimizu
 * @author Mahmoud Ben Hassine
 * @author Lars Uffmann
 * @author Lasse Wulff
 * @author Yanming Zhou
 */
@ExtendWith(OutputCaptureExtension.class)
class BatchAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(BatchAutoConfiguration.class, TransactionManagerCustomizationAutoConfiguration.class,
					TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class));

	@Test
	void testDefaultContext() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JobRepository.class);
			assertThat(context).hasSingleBean(JobOperator.class);
			assertThat(context).hasSingleBean(JobRegistry.class);
			assertThat(context).hasSingleBean(JobOperator.class);
			assertThat(context.getBean(BatchProperties.class).getJdbc().getInitializeSchema())
				.isEqualTo(DatabaseInitializationMode.EMBEDDED);
			assertThat(new JdbcTemplate(context.getBean(DataSource.class))
				.queryForList("select * from BATCH_JOB_EXECUTION")).isEmpty();
		});
	}

	@Test
	void autoconfigurationBacksOffEntirelyIfSpringJdbcAbsent() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withClassLoader(new FilteredClassLoader(DatabasePopulator.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(JobLauncherApplicationRunner.class);
				assertThat(context).doesNotHaveBean(BatchDataSourceScriptDatabaseInitializer.class);
			});
	}

	@Test
	void autoConfigurationBacksOffWhenUserEnablesBatchProcessing() {
		this.contextRunner
			.withUserConfiguration(EnableBatchProcessingConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.withClassLoader(new FilteredClassLoader(DatabasePopulator.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SpringBootBatchConfiguration.class));
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesBatchConfiguration() {
		this.contextRunner.withUserConfiguration(CustomBatchConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.withClassLoader(new FilteredClassLoader(DatabasePopulator.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SpringBootBatchConfiguration.class));
	}

	@Test
	void testDefinesAndLaunchesJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				context.getBean(JobLauncherApplicationRunner.class)
					.run(new DefaultApplicationArguments("jobParam=test"));
				JobParameters jobParameters = new JobParametersBuilder().addString("jobParam", "test")
					.toJobParameters();
				assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters)).isNotNull();
			});
	}

	@Test
	void testDefinesAndLaunchesJobIgnoreOptionArguments() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				context.getBean(JobLauncherApplicationRunner.class)
					.run(new DefaultApplicationArguments("--spring.property=value", "jobParam=test"));
				JobParameters jobParameters = new JobParametersBuilder().addString("jobParam", "test")
					.toJobParameters();
				assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters)).isNotNull();
			});
	}

	@Test
	void testRegisteredAndLocalJob() {
		this.contextRunner
			.withUserConfiguration(NamedJobConfigurationWithRegisteredAndLocalJob.class,
					EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.batch.job.name:discreteRegisteredJob")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				context.getBean(JobLauncherApplicationRunner.class).run();
				assertThat(context.getBean(JobRepository.class)
					.getLastJobExecution("discreteRegisteredJob", new JobParameters())
					.getStatus()).isEqualTo(BatchStatus.COMPLETED);
			});
	}

	@Test
	void testDefinesAndLaunchesLocalJob() {
		this.contextRunner
			.withUserConfiguration(NamedJobConfigurationWithLocalJob.class, EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.batch.job.name:discreteLocalJob")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				context.getBean(JobLauncherApplicationRunner.class).run();
				assertThat(context.getBean(JobRepository.class)
					.getLastJobExecution("discreteLocalJob", new JobParameters())).isNotNull();
			});
	}

	@Test
	void testMultipleJobsAndNoJobName() {
		this.contextRunner.withUserConfiguration(MultipleJobConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure().getCause().getMessage())
					.contains("Job name must be specified in case of multiple jobs");
			});
	}

	@Test
	void testMultipleJobsAndJobName() {
		this.contextRunner.withUserConfiguration(MultipleJobConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.batch.job.name:discreteLocalJob")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				context.getBean(JobLauncherApplicationRunner.class).run();
				assertThat(context.getBean(JobRepository.class)
					.getLastJobExecution("discreteLocalJob", new JobParameters())).isNotNull();
			});
	}

	@Test
	void testDisableLaunchesJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.batch.job.enabled:false")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				assertThat(context).doesNotHaveBean(CommandLineRunner.class);
			});
	}

	@Test
	void testDisableSchemaLoader() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.datasource.generate-unique-name=true",
					"spring.batch.jdbc.initialize-schema:never")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				assertThat(context.getBean(BatchProperties.class).getJdbc().getInitializeSchema())
					.isEqualTo(DatabaseInitializationMode.NEVER);
				assertThat(context).doesNotHaveBean(BatchDataSourceScriptDatabaseInitializer.class);
				assertThatExceptionOfType(BadSqlGrammarException.class)
					.isThrownBy(() -> new JdbcTemplate(context.getBean(DataSource.class))
						.queryForList("select * from BATCH_JOB_EXECUTION"));
			});
	}

	@Test
	void testUsingJpa() {
		this.contextRunner
			.withUserConfiguration(TestJpaConfiguration.class, EmbeddedDataSourceConfiguration.class,
					HibernateJpaAutoConfiguration.class)
			.run((context) -> {
				PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
				// It's a lazy proxy, but it does render its target if you ask for
				// toString():
				assertThat(transactionManager.toString()).contains("JpaTransactionManager");
				assertThat(context).hasSingleBean(EntityManagerFactory.class);
				// Ensure the JobRepository can be used (no problem with isolation
				// level)
				assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", new JobParameters()))
					.isNull();
			});
	}

	@Test
	@WithPackageResources("custom-schema.sql")
	void testRenamePrefix() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.datasource.generate-unique-name=true",
					"spring.batch.jdbc.schema:classpath:custom-schema.sql", "spring.batch.jdbc.table-prefix:PREFIX_")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobOperator.class);
				assertThat(context.getBean(BatchProperties.class).getJdbc().getInitializeSchema())
					.isEqualTo(DatabaseInitializationMode.EMBEDDED);
				assertThat(new JdbcTemplate(context.getBean(DataSource.class))
					.queryForList("select * from PREFIX_JOB_EXECUTION")).isEmpty();
				JobRepository jobRepository = context.getBean(JobRepository.class);
				assertThat(jobRepository.findRunningJobExecutions("test")).isEmpty();
				assertThat(jobRepository.getLastJobExecution("test", new JobParameters())).isNull();
			});
	}

	@Test
	void testCustomizeJpaTransactionManagerUsingProperties() {
		this.contextRunner
			.withUserConfiguration(TestJpaConfiguration.class, EmbeddedDataSourceConfiguration.class,
					HibernateJpaAutoConfiguration.class)
			.withPropertyValues("spring.transaction.default-timeout:30",
					"spring.transaction.rollback-on-commit-failure:true")
			.run((context) -> {
				assertThat(context).hasSingleBean(BatchAutoConfiguration.class);
				JpaTransactionManager transactionManager = JpaTransactionManager.class
					.cast(context.getBean(SpringBootBatchConfiguration.class).getTransactionManager());
				assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
				assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
			});
	}

	@Test
	void testCustomizeDataSourceTransactionManagerUsingProperties() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.transaction.default-timeout:30",
					"spring.transaction.rollback-on-commit-failure:true")
			.run((context) -> {
				assertThat(context).hasSingleBean(SpringBootBatchConfiguration.class);
				DataSourceTransactionManager transactionManager = DataSourceTransactionManager.class
					.cast(context.getBean(SpringBootBatchConfiguration.class).getTransactionManager());
				assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
				assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
			});
	}

	@Test
	void testBatchDataSource() {
		this.contextRunner.withUserConfiguration(BatchDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(SpringBootBatchConfiguration.class)
				.hasSingleBean(BatchDataSourceScriptDatabaseInitializer.class)
				.hasBean("batchDataSource");
			DataSource batchDataSource = context.getBean("batchDataSource", DataSource.class);
			assertThat(context.getBean(SpringBootBatchConfiguration.class).getDataSource()).isEqualTo(batchDataSource);
			assertThat(context.getBean(BatchDataSourceScriptDatabaseInitializer.class))
				.hasFieldOrPropertyWithValue("dataSource", batchDataSource);
		});
	}

	@Test
	void testBatchTransactionManager() {
		this.contextRunner.withUserConfiguration(BatchTransactionManagerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(SpringBootBatchConfiguration.class);
			PlatformTransactionManager batchTransactionManager = context.getBean("batchTransactionManager",
					PlatformTransactionManager.class);
			assertThat(context.getBean(SpringBootBatchConfiguration.class).getTransactionManager())
				.isEqualTo(batchTransactionManager);
		});
	}

	@Test
	void testBatchTaskExecutor() {
		this.contextRunner
			.withUserConfiguration(BatchTaskExecutorConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(SpringBootBatchConfiguration.class).hasBean("batchTaskExecutor");
				TaskExecutor batchTaskExecutor = context.getBean("batchTaskExecutor", TaskExecutor.class);
				assertThat(batchTaskExecutor).isInstanceOf(AsyncTaskExecutor.class);
				assertThat(context.getBean(SpringBootBatchConfiguration.class).getTaskExecutor())
					.isEqualTo(batchTaskExecutor);
				JobOperator jobOperator = AopTestUtils.getTargetObject(context.getBean(JobOperator.class));
				assertThat(jobOperator).hasFieldOrPropertyWithValue("taskExecutor", batchTaskExecutor);
			});
	}

	@Test
	void jobRepositoryBeansDependOnBatchDataSourceInitializer() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			String[] jobRepositoryNames = beanFactory.getBeanNamesForType(JobRepository.class);
			assertThat(jobRepositoryNames).isNotEmpty();
			for (String jobRepositoryName : jobRepositoryNames) {
				assertThat(beanFactory.getBeanDefinition(jobRepositoryName).getDependsOn())
					.contains("batchDataSourceInitializer");
			}
		});
	}

	@Test
	void jobRepositoryBeansDependOnFlyway() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, FlywayAutoConfiguration.class)
			.withPropertyValues("spring.batch.jdbc.initialize-schema=never")
			.run((context) -> {
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				String[] jobRepositoryNames = beanFactory.getBeanNamesForType(JobRepository.class);
				assertThat(jobRepositoryNames).isNotEmpty();
				for (String jobRepositoryName : jobRepositoryNames) {
					assertThat(beanFactory.getBeanDefinition(jobRepositoryName).getDependsOn()).contains("flyway",
							"flywayInitializer");
				}
			});
	}

	@Test
	@WithResource(name = "db/changelog/db.changelog-master.yaml", content = "databaseChangeLog:")
	void jobRepositoryBeansDependOnLiquibase() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, LiquibaseAutoConfiguration.class)
			.withPropertyValues("spring.batch.jdbc.initialize-schema=never")
			.run((context) -> {
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				String[] jobRepositoryNames = beanFactory.getBeanNamesForType(JobRepository.class);
				assertThat(jobRepositoryNames).isNotEmpty();
				for (String jobRepositoryName : jobRepositoryNames) {
					assertThat(beanFactory.getBeanDefinition(jobRepositoryName).getDependsOn()).contains("liquibase");
				}
			});
	}

	@Test
	void whenTheUserDefinesTheirOwnBatchDatabaseInitializerThenTheAutoConfiguredInitializerBacksOff() {
		this.contextRunner.withUserConfiguration(CustomBatchDatabaseInitializerConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(BatchDataSourceScriptDatabaseInitializer.class)
				.doesNotHaveBean("batchDataSourceScriptDatabaseInitializer")
				.hasBean("customInitializer"));
	}

	@Test
	void whenTheUserDefinesTheirOwnDatabaseInitializerThenTheAutoConfiguredBatchInitializerRemains() {
		this.contextRunner.withUserConfiguration(CustomDatabaseInitializerConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(BatchDataSourceScriptDatabaseInitializer.class)
				.hasBean("customInitializer"));
	}

	@Test
	void conversionServiceCustomizersAreCalled() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
					ConversionServiceCustomizersConfiguration.class)
			.run((context) -> {
				BatchConversionServiceCustomizer customizer = context.getBean("batchConversionServiceCustomizer",
						BatchConversionServiceCustomizer.class);
				BatchConversionServiceCustomizer anotherCustomizer = context
					.getBean("anotherBatchConversionServiceCustomizer", BatchConversionServiceCustomizer.class);
				InOrder inOrder = Mockito.inOrder(customizer, anotherCustomizer);
				ConfigurableConversionService configurableConversionService = context
					.getBean(SpringBootBatchConfiguration.class)
					.getConversionService();
				inOrder.verify(customizer).customize(configurableConversionService);
				inOrder.verify(anotherCustomizer).customize(configurableConversionService);
			});
	}

	@Test
	void whenTheUserDefinesAJobNameAsJobInstanceValidates() {
		JobLauncherApplicationRunner runner = createInstance("another");
		runner.setJobs(Collections.singletonList(mockJob("test")));
		runner.setJobName("test");
		runner.afterPropertiesSet();
	}

	@Test
	void whenTheUserDefinesAJobNameAsRegisteredJobValidates() {
		JobLauncherApplicationRunner runner = createInstance("test");
		runner.setJobName("test");
		runner.afterPropertiesSet();
	}

	@Test
	void whenTheUserDefinesAJobNameThatDoesNotExistWithJobInstancesFailsFast() {
		JobLauncherApplicationRunner runner = createInstance();
		runner.setJobs(Arrays.asList(mockJob("one"), mockJob("two")));
		runner.setJobName("three");
		assertThatIllegalStateException().isThrownBy(runner::afterPropertiesSet)
			.withMessage("No job found with name 'three'");
	}

	@Test
	void whenTheUserDefinesAJobNameThatDoesNotExistWithRegisteredJobFailsFast() {
		JobLauncherApplicationRunner runner = createInstance("one", "two");
		runner.setJobName("three");
		assertThatIllegalStateException().isThrownBy(runner::afterPropertiesSet)
			.withMessage("No job found with name 'three'");
	}

	@Test
	void customExecutionContextSerializerIsUsed() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withBean(ExecutionContextSerializer.class, Jackson2ExecutionContextStringSerializer::new)
			.run((context) -> {
				assertThat(context).hasSingleBean(Jackson2ExecutionContextStringSerializer.class);
				assertThat(context.getBean(SpringBootBatchConfiguration.class).getExecutionContextSerializer())
					.isInstanceOf(Jackson2ExecutionContextStringSerializer.class);
			});
	}

	@Test
	void defaultExecutionContextSerializerIsUsed() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ExecutionContextSerializer.class);
			assertThat(context.getBean(SpringBootBatchConfiguration.class).getExecutionContextSerializer())
				.isInstanceOf(DefaultExecutionContextSerializer.class);
		});
	}

	@Test
	void customJdbcPropertiesIsUsed() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.batch.jdbc.validate-transaction-state:false",
					"spring.batch.jdbc.isolation-level-for-create:READ_COMMITTED")
			.run((context) -> {
				SpringBootBatchConfiguration configuration = context.getBean(SpringBootBatchConfiguration.class);
				assertThat(configuration.getValidateTransactionState()).isEqualTo(false);
				assertThat(configuration.getIsolationLevelForCreate()).isEqualTo(Isolation.READ_COMMITTED);
			});

	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void customJobParametersConverterIsUsed() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withBean(JobParametersConverter.class, JsonJobParametersConverter::new)
			.withPropertyValues("spring.datasource.generate-unique-name=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(JsonJobParametersConverter.class);
				assertThat(context.getBean(SpringBootBatchConfiguration.class).getJobParametersConverter())
					.isInstanceOf(JsonJobParametersConverter.class);
			});
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void defaultJobParametersConverterIsUsed() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(JobParametersConverter.class);
			assertThat(context.getBean(SpringBootBatchConfiguration.class).getJobParametersConverter())
				.isInstanceOf(DefaultJobParametersConverter.class);
		});
	}

	private JobLauncherApplicationRunner createInstance(String... registeredJobNames) {
		JobLauncherApplicationRunner runner = new JobLauncherApplicationRunner(mock(JobOperator.class));
		JobRegistry jobRegistry = mock(JobRegistry.class);
		given(jobRegistry.getJobNames()).willReturn(Arrays.asList(registeredJobNames));
		runner.setJobRegistry(jobRegistry);
		return runner;
	}

	private Job mockJob(String name) {
		Job job = mock(Job.class);
		given(job.getName()).willReturn(name);
		return job;
	}

	@Configuration(proxyBeanMethods = false)
	static class BatchDataSourceConfiguration {

		@Bean
		DataSource normalDataSource() {
			return DataSourceBuilder.create().url("jdbc:h2:mem:normal").username("sa").build();
		}

		@BatchDataSource
		@Bean(defaultCandidate = false)
		DataSource batchDataSource() {
			return DataSourceBuilder.create().url("jdbc:h2:mem:batchdatasource").username("sa").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BatchTransactionManagerConfiguration {

		@Bean
		DataSource dataSource() {
			return DataSourceBuilder.create().url("jdbc:h2:mem:database").username("sa").build();
		}

		@Bean
		@Primary
		PlatformTransactionManager normalTransactionManager() {
			return mock(PlatformTransactionManager.class);
		}

		@BatchTransactionManager
		@Bean(defaultCandidate = false)
		PlatformTransactionManager batchTransactionManager() {
			return mock(PlatformTransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BatchTaskExecutorConfiguration {

		@Bean
		TaskExecutor taskExecutor() {
			return new SyncTaskExecutor();
		}

		@BatchTaskExecutor
		@Bean(defaultCandidate = false)
		TaskExecutor batchTaskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@TestAutoConfigurationPackage(City.class)
	static class TestJpaConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class EntityManagerFactoryConfiguration {

		@Bean
		EntityManagerFactory entityManagerFactory() {
			return mock(EntityManagerFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NamedJobConfigurationWithRegisteredAndLocalJob {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteRegisteredJob") {

				private static int count = 0;

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution) {
					if (count == 0) {
						execution.setStatus(BatchStatus.COMPLETED);
					}
					else {
						execution.setStatus(BatchStatus.FAILED);
					}
					count++;
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NamedJobConfigurationWithLocalJob {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteLocalJob") {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleJobConfiguration {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteLocalJob") {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

		@Bean
		Job job2() {
			return new Job() {
				@Override
				public String getName() {
					return "discreteLocalJob2";
				}

				@Override
				public void execute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JobConfiguration {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job job() {
			AbstractJob job = new AbstractJob() {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomBatchDatabaseInitializerConfiguration {

		@Bean
		BatchDataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource, BatchProperties properties) {
			return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDatabaseInitializerConfiguration {

		@Bean
		DataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource) {
			return new DataSourceScriptDatabaseInitializer(dataSource, new DatabaseInitializationSettings());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomBatchConfiguration extends DefaultBatchConfiguration {

	}

	@EnableBatchProcessing
	@Configuration(proxyBeanMethods = false)
	static class EnableBatchProcessingConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ConversionServiceCustomizersConfiguration {

		@Bean
		@Order(1)
		BatchConversionServiceCustomizer batchConversionServiceCustomizer() {
			return mock(BatchConversionServiceCustomizer.class);
		}

		@Bean
		@Order(2)
		BatchConversionServiceCustomizer anotherBatchConversionServiceCustomizer() {
			return mock(BatchConversionServiceCustomizer.class);
		}

	}

}
