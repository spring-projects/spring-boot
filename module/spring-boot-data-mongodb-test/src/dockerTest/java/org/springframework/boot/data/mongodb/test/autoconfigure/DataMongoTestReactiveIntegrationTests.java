/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample tests for {@link DataMongoTest @DataMongoTest} using reactive repositories.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
class DataMongoTestReactiveIntegrationTests {

	@Container
	@ServiceConnection
	static final MongoDBContainer mongoDb = TestImage.forContainer(MongoDBContainer.class);

	@Autowired
	private ReactiveMongoTemplate mongoTemplate;

	@Autowired
	private ExampleReactiveRepository exampleRepository;

	@Test
	void testRepository() {
		ExampleDocument exampleDocument = new ExampleDocument();
		exampleDocument.setText("Look, new @DataMongoTest!");
		exampleDocument = this.exampleRepository.save(exampleDocument).block(Duration.ofSeconds(30));
		assertThat(exampleDocument).isNotNull();
		assertThat(exampleDocument.getId()).isNotNull();
		assertThat(this.mongoTemplate.collectionExists("exampleDocuments").block(Duration.ofSeconds(30))).isTrue();
	}

}
