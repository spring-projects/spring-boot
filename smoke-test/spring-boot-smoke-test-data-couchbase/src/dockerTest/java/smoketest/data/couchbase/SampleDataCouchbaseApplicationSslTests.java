/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.couchbase;

import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.couchbase.test.autoconfigure.DataCouchbaseTest;
import org.springframework.boot.testcontainers.service.connection.PemKeyStore;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.couchbase.core.CouchbaseTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Couchbase with SSL.
 *
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
@DataCouchbaseTest(
		properties = { "spring.couchbase.env.timeouts.connect=2m", "spring.data.couchbase.bucket-name=cbbucket" })
class SampleDataCouchbaseApplicationSslTests {

	private static final String BUCKET_NAME = "cbbucket";

	@Container
	@ServiceConnection
	@PemKeyStore(certificate = "classpath:ssl/test-client.crt", privateKey = "classpath:ssl/test-client.key")
	@PemTrustStore(certificate = "classpath:ssl/test-ca.crt")
	static final CouchbaseContainer couchbase = TestImage.forContainer(SecureCouchbaseContainer.class)
		.withBucket(new BucketDefinition(BUCKET_NAME));

	@Autowired
	private CouchbaseTemplate couchbaseTemplate;

	@Autowired
	private SampleRepository repository;

	@Test
	void testRepository() {
		SampleDocument document = new SampleDocument();
		document.setText("Look, new @DataCouchbaseTest!");
		document = this.repository.save(document);
		assertThat(document.getId()).isNotNull();
		assertThat(this.couchbaseTemplate.getBucketName()).isEqualTo(BUCKET_NAME);
		this.repository.deleteAll();
	}

}
