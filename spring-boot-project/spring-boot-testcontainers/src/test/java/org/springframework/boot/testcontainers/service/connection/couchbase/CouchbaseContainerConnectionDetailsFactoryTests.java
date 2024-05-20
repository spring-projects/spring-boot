/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection.couchbase;

import java.time.Duration;

import com.couchbase.client.java.Cluster;
import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.couchbase.CouchbaseService;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
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
	static final CouchbaseContainer couchbase = new CouchbaseContainer(DockerImageNames.couchbase())
		.withEnabledServices(CouchbaseService.KV, CouchbaseService.INDEX, CouchbaseService.QUERY)
		.withStartupAttempts(5)
		.withStartupTimeout(Duration.ofMinutes(10))
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
