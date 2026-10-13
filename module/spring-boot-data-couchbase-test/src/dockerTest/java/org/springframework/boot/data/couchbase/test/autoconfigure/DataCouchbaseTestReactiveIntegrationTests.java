/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.test.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.couchbase.CouchbaseService;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
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
	static final CouchbaseContainer couchbase = TestImage.forContainer(CouchbaseContainer.class)
		.withEnabledServices(CouchbaseService.KV, CouchbaseService.INDEX, CouchbaseService.QUERY)
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
		assertThat(document).isNotNull();
		assertThat(document.getId()).isNotNull();
		assertThat(this.couchbaseTemplate.getBucketName()).isEqualTo(BUCKET_NAME);
		this.exampleReactiveRepository.deleteAll();
	}

}
