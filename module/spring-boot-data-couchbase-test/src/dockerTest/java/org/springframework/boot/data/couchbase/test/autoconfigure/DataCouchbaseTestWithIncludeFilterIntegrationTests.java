/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.test.autoconfigure;

import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.couchbase.CouchbaseService;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
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
@DataCouchbaseTest(includeFilters = @Filter(Service.class),
		properties = { "spring.data.couchbase.bucket-name=cbbucket", "spring.couchbase.env.timeouts.connect=2m",
				"spring.couchbase.env.timeouts.key-value=1m" })
@Testcontainers(disabledWithoutDocker = true)
class DataCouchbaseTestWithIncludeFilterIntegrationTests {

	@Container
	@ServiceConnection
	static final CouchbaseContainer couchbase = TestImage.forContainer(CouchbaseContainer.class)
		.withEnabledServices(CouchbaseService.KV, CouchbaseService.INDEX, CouchbaseService.QUERY)
		.withBucket(new BucketDefinition("cbbucket"));

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
