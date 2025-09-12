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

import com.mongodb.client.MongoClient;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.configuration.support.MongoDefaultBatchConfiguration;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchJobLauncherAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.boot.batch.autoconfigure.BatchTransactionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.transaction.annotation.Isolation;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Batch using Data MongoDB.
 *
 * @author Stephane Nicoll
 * @since 4.1.0
 */
@AutoConfiguration(before = { BatchAutoConfiguration.class, BatchJobLauncherAutoConfiguration.class },
		after = DataMongoAutoConfiguration.class)
@ConditionalOnClass({ JobOperator.class, MongoClient.class, MongoOperations.class })
@ConditionalOnBean(MongoDatabaseFactory.class)
@ConditionalOnMissingBean(value = DefaultBatchConfiguration.class, annotation = EnableBatchProcessing.class)
@EnableConfigurationProperties(BatchDataMongoProperties.class)
public final class BatchDataMongoAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty("spring.batch.data.mongodb.schema.initialize")
	@ConditionalOnBean(MongoOperations.class)
	@Import(JobRepositoryDependsOnSchemaInitializationDetector.class)
	static class BatchMongoDatabaseInitializerConfiguration {

		@Bean(name = JobRepositoryDependsOnSchemaInitializationDetector.SCHEMA_INITIALIZATION_BEAN_NAME)
		InitializingBean batchMongoDataInitializingBean(MongoOperations mongoOperations,
				BatchDataMongoProperties properties) {
			return () -> {
				BatchMongoSchemaInitializer initializer = new BatchMongoSchemaInitializer(mongoOperations);
				String schemaLocation = properties.getSchema().getLocation();
				Resource resource = new ClassPathResource(schemaLocation);
				if (!resource.exists()) {
					throw new InvalidConfigurationPropertyValueException("spring.batch.data.mongodb.schema.location",
							schemaLocation, "resource does not exist");
				}
				initializer.initialize(resource);
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SpringBootBatchMongoConfiguration extends MongoDefaultBatchConfiguration {

		private final MongoOperations mongoOperations;

		private final MongoTransactionManager transactionManager;

		private final @Nullable TaskExecutor taskExecutor;

		private final BatchDataMongoProperties properties;

		SpringBootBatchMongoConfiguration(MongoDatabaseFactory mongoDatabaseFactory,
				ObjectProvider<MongoTransactionManager> transactionManager,
				@BatchTransactionManager ObjectProvider<MongoTransactionManager> batchTransactionManager,
				@BatchTaskExecutor ObjectProvider<TaskExecutor> batchTaskExecutor,
				BatchDataMongoProperties properties) {
			this.mongoOperations = createMongoOperations(mongoDatabaseFactory);
			this.transactionManager = batchTransactionManager.getIfAvailable(
					() -> transactionManager.getIfAvailable(() -> new MongoTransactionManager(mongoDatabaseFactory)));
			this.taskExecutor = batchTaskExecutor.getIfAvailable();
			this.properties = properties;
		}

		private static MongoTemplate createMongoOperations(MongoDatabaseFactory mongoDatabaseFactory) {
			MongoTemplate template = new MongoTemplate(mongoDatabaseFactory);
			MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
			converter.setMapKeyDotReplacement(".");
			return template;
		}

		@Override
		protected MongoOperations getMongoOperations() {
			return this.mongoOperations;
		}

		@Override
		protected MongoTransactionManager getTransactionManager() {
			return this.transactionManager;
		}

		@Override
		protected boolean getValidateTransactionState() {
			return this.properties.isValidateTransactionState();
		}

		@Override
		protected Isolation getIsolationLevelForCreate() {
			Isolation isolation = this.properties.getIsolationLevelForCreate();
			return (isolation != null) ? isolation : super.getIsolationLevelForCreate();
		}

		@Override
		protected TaskExecutor getTaskExecutor() {
			return (this.taskExecutor != null) ? this.taskExecutor : super.getTaskExecutor();
		}

	}

}
