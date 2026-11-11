/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.testcontainers;

import com.couchbase.client.java.Cluster;
import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.couchbase.CouchbaseService;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class CouchbaseContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final CouchbaseContainer couchbase = TestImage.forContainer(CouchbaseContainer.class)
		.withEnabledServices(CouchbaseService.KV, CouchbaseService.INDEX, CouchbaseService.QUERY)
		.withBucket(new BucketDefinition("cbbucket"));

	@Autowired(required = false)
	private CouchbaseConnectionDetails connectionDetails;

	@Autowired
	private Cluster cluster;

	@Test
	void connectionCanBeMadeToCouchbaseContainer() {
		assertThat(this.connectionDetails).isNotNull();
		assertThat(this.cluster.diagnostics().endpoints()).hasSize(1);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(CouchbaseAutoConfiguration.class)
	static class TestConfiguration {

	}

}
