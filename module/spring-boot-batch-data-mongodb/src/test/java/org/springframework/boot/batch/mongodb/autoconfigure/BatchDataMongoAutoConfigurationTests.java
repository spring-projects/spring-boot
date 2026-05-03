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

package org.springframework.boot.batch.mongodb.autoconfigure;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.batch.autoconfigure.BatchTransactionManager;
import org.springframework.boot.batch.mongodb.autoconfigure.BatchDataMongoAutoConfiguration.SpringBootBatchMongoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.data.mongodb.core.MongoOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link BatchDataMongoAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class BatchDataMongoAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BatchDataMongoAutoConfiguration.class));

	@Test
	void autoConfigurationWithSpringDataMongoDb() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, DataMongoAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(JobRepository.class).hasSingleBean(JobOperator.class));
	}

	@Test
	void autoConfigurationOnlyRequiresMongoDatabaseFactory() {
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.run((context) -> assertThat(context).hasSingleBean(JobRepository.class).hasSingleBean(JobOperator.class));
	}

	@Test
	void autConfigurationUsesMainTransactionManager() {
		MongoTransactionManager transactionManager = mock(MongoTransactionManager.class);
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.withBean(MongoTransactionManager.class, () -> transactionManager)
			.run((context) -> assertThat(
					context.getBean(SpringBootBatchMongoConfiguration.class).getTransactionManager())
				.isSameAs(transactionManager));
	}

	@Test
	void autConfigurationFavorsBatchTransactionManager() {
		MongoTransactionManager transactionManager = mock(MongoTransactionManager.class);
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.withBean(MongoTransactionManager.class, () -> transactionManager)
			.withUserConfiguration(BatchTransactionManagerConfiguration.class)
			.run((context) -> {
				assertThat(context.getBeansOfType(MongoTransactionManager.class)).hasSize(2);
				assertThat(context.getBean(SpringBootBatchMongoConfiguration.class).getTransactionManager())
					.isSameAs(context.getBean("customTransactionManager"));
			});
	}

	@Test
	void autConfigurationCreatesBatchTransactionManagerIfNecessary() {
		MongoDatabaseFactory mongoDatabaseFactory = mockMongoDatabaseFactory();
		this.contextRunner.withBean(MongoDatabaseFactory.class, () -> mongoDatabaseFactory).run((context) -> {
			assertThat(context).doesNotHaveBean(MongoTransactionManager.class);
			assertThat(context.getBean(SpringBootBatchMongoConfiguration.class).getTransactionManager())
				.satisfies((mongoTransactionManager) -> assertThat(mongoTransactionManager.getDatabaseFactory())
					.isSameAs(mongoDatabaseFactory));
		});
	}

	@Test
	void autoconfigurationBacksOffEntirelyIfSpringMongoDbAbsent() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, DataMongoAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(MongoOperations.class))
			.run((context) -> assertThat(context).doesNotHaveBean(JobRepository.class)
				.doesNotHaveBean(JobOperator.class));
	}

	@Test
	void autoConfigurationBacksOfEntirelyIfSpringMongoDbIsNotConfigured() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(JobRepository.class)
			.doesNotHaveBean(JobOperator.class));
	}

	@Test
	void autoConfigurationBacksOffWhenUserEnablesBatchProcessing() {
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.withUserConfiguration(EnableBatchProcessingConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(SpringBootBatchMongoConfiguration.class));
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesBatchConfiguration() {
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.withUserConfiguration(CustomBatchConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(SpringBootBatchMongoConfiguration.class));
	}

	@Test
	void schemaInitializerBeanNotCreatedByDefault() {
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.run((context) -> assertThat(context)
				.doesNotHaveBean(JobRepositoryDependsOnSchemaInitializationDetector.SCHEMA_INITIALIZATION_BEAN_NAME));
	}

	@Test
	void schemaInitializerBeanCreatedWhenSchemaInitializeEnabled() {
		MongoOperations mongoOperations = mock(MongoOperations.class);
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.withBean(MongoOperations.class, () -> mongoOperations)
			.withPropertyValues("spring.batch.data.mongodb.schema.initialize=true")
			.run((context) -> {
				assertThat(context)
					.hasBean(JobRepositoryDependsOnSchemaInitializationDetector.SCHEMA_INITIALIZATION_BEAN_NAME);
				// see org/springframework/batch/core/schema-mongodb.jsonl
				then(mongoOperations).should(times(7)).executeCommand(anyString());
			});
	}

	@Test
	void jobRepositoryDependsOnSchemaInitializerWhenSchemaInitializationEnabled() {
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.withBean(MongoOperations.class, Mockito::mock)
			.withPropertyValues("spring.batch.data.mongodb.schema.initialize=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(JobRepository.class);
				BeanDefinition jobRepositoryDefinition = context.getBeanFactory().getBeanDefinition("jobRepository");
				assertThat(jobRepositoryDefinition.getDependsOn())
					.contains(JobRepositoryDependsOnSchemaInitializationDetector.SCHEMA_INITIALIZATION_BEAN_NAME);
			});
	}

	@Test
	void schemaInitializationFailsWhenSchemaLocationDoesNotExist() {
		this.contextRunner.withBean(MongoDatabaseFactory.class, this::mockMongoDatabaseFactory)
			.withBean(MongoOperations.class, Mockito::mock)
			.withPropertyValues("spring.batch.data.mongodb.schema.initialize=true",
					"spring.batch.data.mongodb.schema.location=classpath:does/not/exist.jsonl")
			.run((context) -> assertThat(context).getFailure()
				.hasRootCauseInstanceOf(
						org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException.class)
				.rootCause()
				.hasMessageContaining("spring.batch.data.mongodb.schema.location")
				.hasMessageContaining("resource does not exist"));
	}

	private MongoDatabaseFactory mockMongoDatabaseFactory() {
		MongoDatabaseFactory factory = mock(MongoDatabaseFactory.class);
		given(factory.getExceptionTranslator()).willReturn(MongoExceptionTranslator.DEFAULT_EXCEPTION_TRANSLATOR);
		return factory;
	}

	@EnableBatchProcessing
	@Configuration(proxyBeanMethods = false)
	static class EnableBatchProcessingConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomBatchConfiguration extends DefaultBatchConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class BatchTransactionManagerConfiguration {

		@Bean
		@BatchTransactionManager
		MongoTransactionManager customTransactionManager() {
			return mock(MongoTransactionManager.class);
		}

	}

}
