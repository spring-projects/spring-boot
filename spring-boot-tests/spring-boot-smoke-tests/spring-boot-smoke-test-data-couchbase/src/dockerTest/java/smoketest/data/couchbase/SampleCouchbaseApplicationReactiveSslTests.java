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

package smoketest.data.couchbase;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.PemKeyStore;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.couchbase.core.ReactiveCouchbaseTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Couchbase using reactive repositories with SSL.
 *
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = { "spring.data.couchbase.bucket-name=cbbucket" })
class SampleCouchbaseApplicationReactiveSslTests {

	private static final String BUCKET_NAME = "cbbucket";

	@Container
	@ServiceConnection
	@PemKeyStore(certificate = "classpath:ssl/test-client.crt", privateKey = "classpath:ssl/test-client.key")
	@PemTrustStore(certificate = "classpath:ssl/test-ca.crt")
	static final CouchbaseContainer couchbase = TestImage.container(SecureCouchbaseContainer.class)
		.withBucket(new BucketDefinition(BUCKET_NAME));

	@Autowired
	private ReactiveCouchbaseTemplate couchbaseTemplate;

	@Autowired
	private SampleReactiveRepository repository;

	@Test
	void testRepository() {
		SampleDocument document = new SampleDocument();
		document.setText("Look, new @DataCouchbaseTest!");
		document = this.repository.save(document).block(Duration.ofSeconds(30));
		assertThat(document.getId()).isNotNull();
		assertThat(this.couchbaseTemplate.getBucketName()).isEqualTo(BUCKET_NAME);
		this.repository.deleteAll();
	}

}
