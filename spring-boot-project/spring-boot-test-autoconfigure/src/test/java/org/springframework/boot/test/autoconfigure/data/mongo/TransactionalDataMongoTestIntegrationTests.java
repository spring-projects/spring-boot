/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.mongo;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for using {@link DataMongoTest @DataMongoTest} with transactions.
 *
 * @author Andy Wilkinson
 */
@DataMongoTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class TransactionalDataMongoTestIntegrationTests {

	@Container
	static final MongoDBContainer mongoDB = new MongoDBContainer(DockerImageNames.mongo()).withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(5));

	@Autowired
	private ExampleRepository exampleRepository;

	@Test
	void testRepository() {
		ExampleDocument exampleDocument = new ExampleDocument();
		exampleDocument.setText("Look, new @DataMongoTest!");
		exampleDocument = this.exampleRepository.save(exampleDocument);
		assertThat(exampleDocument.getId()).isNotNull();
	}

	@DynamicPropertySource
	static void mongoProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TransactionManagerConfiguration {

		@Bean
		MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
			return new MongoTransactionManager(dbFactory);
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class MongoInitializationConfiguration {

		@Bean
		MongoInitializer mongoInitializer(MongoTemplate template) {
			return new MongoInitializer(template);
		}

		static class MongoInitializer implements InitializingBean {

			private final MongoTemplate template;

			MongoInitializer(MongoTemplate template) {
				this.template = template;
			}

			@Override
			public void afterPropertiesSet() throws Exception {
				this.template.createCollection("exampleDocuments");
			}

		}

	}

}
