/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.mongodb;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.PemKeyStore;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Data MongoDB with SSL.
 *
 * @author Scott Frederick
 * @author Eddú Meléndez
 */
@Testcontainers(disabledWithoutDocker = true)
@DataMongoTest
class SampleMongoApplicationSslTests {

	@Container
	@ServiceConnection
	@PemKeyStore(certificate = "classpath:ssl/test-client.crt", privateKey = "classpath:ssl/test-client.key")
	@PemTrustStore("classpath:ssl/test-ca.crt")
	static final MongoDBContainer mongoDb = TestImage.forContainer(SecureMongoContainer.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private SampleRepository exampleRepository;

	@Test
	void testRepository() {
		SampleDocument exampleDocument = new SampleDocument();
		exampleDocument.setText("Look, new @DataMongoTest!");
		exampleDocument = this.exampleRepository.save(exampleDocument);
		assertThat(exampleDocument.getId()).isNotNull();
		assertThat(this.mongoTemplate.collectionExists("exampleDocuments")).isTrue();
	}

}
