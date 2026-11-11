/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.test.autoconfigure;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test with custom include filter for {@link DataRedisTest @DataRedisTest}.
 *
 * @author Jayaram Pradhan
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Testcontainers(disabledWithoutDocker = true)
@DataRedisTest(includeFilters = @Filter(Service.class))
class DataRedisTestWithIncludeFilterIntegrationTests {

	@Container
	@ServiceConnection
	static final RedisContainer redis = TestImage.forContainer(RedisContainer.class);

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ExampleService service;

	@Test
	void testService() {
		PersonHash personHash = new PersonHash();
		personHash.setDescription("Look, new @DataRedisTest!");
		assertThat(personHash.getId()).isNull();
		PersonHash savedEntity = this.exampleRepository.save(personHash);
		assertThat(this.service.hasRecord(savedEntity)).isTrue();
	}

}
