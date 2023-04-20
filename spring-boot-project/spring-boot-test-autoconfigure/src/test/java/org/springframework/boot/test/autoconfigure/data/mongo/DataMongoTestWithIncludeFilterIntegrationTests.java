/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
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
	static final MongoDBContainer mongoDB = new MongoDBContainer(DockerImageNames.mongo()).withStartupAttempts(5)
		.withStartupTimeout(Duration.ofMinutes(5));

	@Autowired
	private ExampleService service;

	@Test
	void testService() {
		assertThat(this.service.hasCollection("foobar")).isFalse();
	}

}
