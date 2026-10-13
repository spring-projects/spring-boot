/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Sample test for {@link DataMongoTest @DataMongoTest}.
 *
 * @author Michael Simons
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
class DataMongoTestIntegrationTests {

	@Container
	@ServiceConnection
	static final MongoDBContainer mongoDb = TestImage.forContainer(MongoDBContainer.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testRepository() {
		ExampleDocument exampleDocument = new ExampleDocument();
		exampleDocument.setText("Look, new @DataMongoTest!");
		exampleDocument = this.exampleRepository.save(exampleDocument);
		assertThat(exampleDocument.getId()).isNotNull();
		assertThat(this.mongoTemplate.collectionExists("exampleDocuments")).isTrue();
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@Test
	void serviceConnectionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ServiceConnectionAutoConfiguration.class));
	}

	@Test
	void transactionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(TransactionAutoConfiguration.class));
		assertThat(this.applicationContext)
			.has(importedAutoConfiguration(TransactionManagerCustomizationAutoConfiguration.class));
	}

}
