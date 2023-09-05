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
import org.testcontainers.couchbase.CouchbaseService;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.data.couchbase.core.ReactiveCouchbaseTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample tests for {@link DataCouchbaseTest @DataCouchbaseTest} using reactive
 * repositories.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataCouchbaseTest(properties = { "spring.data.couchbase.bucket-name=cbbucket",
		"spring.couchbase.env.timeouts.connect=2m", "spring.couchbase.env.timeouts.key-value=1m" })
@Testcontainers(disabledWithoutDocker = true)
class DataCouchbaseTestReactiveIntegrationTests {

	private static final String BUCKET_NAME = "cbbucket";

	@Container
	@ServiceConnection
	static final CouchbaseContainer couchbase = new CouchbaseContainer(DockerImageNames.couchbase())
		.withEnabledServices(CouchbaseService.KV, CouchbaseService.INDEX, CouchbaseService.QUERY)
		.withStartupAttempts(5)
		.withStartupTimeout(Duration.ofMinutes(10))
		.withBucket(new BucketDefinition(BUCKET_NAME));

	@Autowired
	private ReactiveCouchbaseTemplate couchbaseTemplate;

	@Autowired
	private ExampleReactiveRepository exampleReactiveRepository;

	@Test
	void testRepository() {
		ExampleDocument document = new ExampleDocument();
		document.setText("Look, new @DataCouchbaseTest!");
		document = this.exampleReactiveRepository.save(document).block(Duration.ofSeconds(30));
		assertThat(document.getId()).isNotNull();
		assertThat(this.couchbaseTemplate.getBucketName()).isEqualTo(BUCKET_NAME);
		this.exampleReactiveRepository.deleteAll();
	}

}
