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

package smoketest.data.mongo;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for MongoDB using reactive repositories with SSL.
 *
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = { "spring.data.mongodb.ssl.bundle=client",
		"spring.ssl.bundle.pem.client.keystore.certificate=classpath:ssl/test-client.crt",
		"spring.ssl.bundle.pem.client.keystore.private-key=classpath:ssl/test-client.key",
		"spring.ssl.bundle.pem.client.truststore.certificate=classpath:ssl/test-ca.crt" })
class SampleMongoApplicationReactiveSslTests {

	@Container
	@ServiceConnection
	static final MongoDBContainer mongoDB = new SecureMongoContainer();

	@Autowired
	private ReactiveMongoTemplate mongoTemplate;

	@Autowired
	private SampleReactiveRepository exampleRepository;

	@Test
	void testRepository() {
		SampleDocument exampleDocument = new SampleDocument();
		exampleDocument.setText("Look, new @DataMongoTest!");
		exampleDocument = this.exampleRepository.save(exampleDocument).block(Duration.ofSeconds(30));
		assertThat(exampleDocument.getId()).isNotNull();
		assertThat(this.mongoTemplate.collectionExists("exampleDocuments").block(Duration.ofSeconds(30))).isTrue();
	}

}
