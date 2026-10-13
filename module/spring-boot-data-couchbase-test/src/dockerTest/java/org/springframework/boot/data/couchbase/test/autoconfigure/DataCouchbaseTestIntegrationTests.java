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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ApplicationContext;
import org.springframework.data.couchbase.core.CouchbaseTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration test for {@link DataCouchbaseTest @DataCouchbaseTest}.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataCouchbaseTest(properties = { "spring.couchbase.env.timeouts.connect=2m",
		"spring.couchbase.env.timeouts.key-value=1m", "spring.data.couchbase.bucket-name=cbbucket" })
@Testcontainers(disabledWithoutDocker = true)
class DataCouchbaseTestIntegrationTests {

	private static final String BUCKET_NAME = "cbbucket";

	@Container
	@ServiceConnection
	static final CouchbaseContainer couchbase = TestImage.forContainer(CouchbaseContainer.class)
		.withEnabledServices(CouchbaseService.KV, CouchbaseService.INDEX, CouchbaseService.QUERY)
		.withBucket(new BucketDefinition(BUCKET_NAME));

	@Autowired
	private CouchbaseTemplate couchbaseTemplate;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@Test
	void testRepository() {
		ExampleDocument document = new ExampleDocument();
		document.setText("Look, new @DataCouchbaseTest!");
		document = this.exampleRepository.save(document);
		assertThat(document.getId()).isNotNull();
		assertThat(this.couchbaseTemplate.getBucketName()).isEqualTo(BUCKET_NAME);
		this.exampleRepository.deleteAll();
	}

	@Test
	void serviceConnectionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ServiceConnectionAutoConfiguration.class));
	}

}
