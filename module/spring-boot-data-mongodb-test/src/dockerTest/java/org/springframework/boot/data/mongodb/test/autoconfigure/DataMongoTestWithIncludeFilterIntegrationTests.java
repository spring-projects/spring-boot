/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test with custom include filter for {@link DataMongoTest @DataMongoTest}.
 *
 * @author Michael Simons
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataMongoTest(includeFilters = @Filter(Service.class))
@Testcontainers(disabledWithoutDocker = true)
class DataMongoTestWithIncludeFilterIntegrationTests {

	@Container
	@ServiceConnection
	static final MongoDBContainer mongoDb = TestImage.forContainer(MongoDBContainer.class);

	@Autowired
	private ExampleService service;

	@Test
	void testService() {
		assertThat(this.service.hasCollection("foobar")).isFalse();
	}

}
