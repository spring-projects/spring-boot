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

package org.springframework.boot.test.autoconfigure.data.couchbase;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test with custom include filter for
 * {@link DataCouchbaseTest @DataCouchbaseTest}.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataCouchbaseTest(includeFilters = @Filter(Service.class), properties = "spring.data.couchbase.bucket-name=cbbucket")
@Testcontainers(disabledWithoutDocker = true)
class DataCouchbaseTestWithIncludeFilterIntegrationTests {

	private static final String BUCKET_NAME = "cbbucket";

	@Container
	@ServiceConnection
	static final CouchbaseContainer couchbase = new CouchbaseContainer(DockerImageNames.couchbase())
		.withStartupAttempts(5)
		.withStartupTimeout(Duration.ofMinutes(10))
		.withBucket(new BucketDefinition(BUCKET_NAME));

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ExampleService service;

	@Test
	void testService() {
		ExampleDocument document = new ExampleDocument();
		document.setText("Look, new @DataCouchbaseTest!");
		document = this.exampleRepository.save(document);
		assertThat(this.service.findById(document.getId())).isNotNull();
	}

}
