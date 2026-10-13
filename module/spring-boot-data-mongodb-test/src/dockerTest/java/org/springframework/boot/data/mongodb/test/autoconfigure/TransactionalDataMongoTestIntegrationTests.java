/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for using {@link DataMongoTest @DataMongoTest} with transactions.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
@DataMongoTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class TransactionalDataMongoTestIntegrationTests {

	@Container
	@ServiceConnection
	static final MongoDBContainer mongoDb = TestImage.forContainer(MongoDBContainer.class).withReplicaSet();

	@Autowired
	private ExampleRepository exampleRepository;

	@Test
	void testRepository() {
		ExampleDocument exampleDocument = new ExampleDocument();
		exampleDocument.setText("Look, new @DataMongoTest!");
		exampleDocument = this.exampleRepository.save(exampleDocument);
		assertThat(exampleDocument.getId()).isNotNull();
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
