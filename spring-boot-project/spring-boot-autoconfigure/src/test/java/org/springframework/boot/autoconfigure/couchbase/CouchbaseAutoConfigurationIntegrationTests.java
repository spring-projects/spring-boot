/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import java.time.Duration;

import com.couchbase.client.core.diagnostics.ClusterState;
import com.couchbase.client.core.diagnostics.DiagnosticsResult;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CouchbaseAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 */
@Testcontainers(disabledWithoutDocker = true)
class CouchbaseAutoConfigurationIntegrationTests {

	private static final String BUCKET_NAME = "cbbucket";

	@Container
	static final CouchbaseContainer couchbase = new CouchbaseContainer(DockerImageNames.couchbase())
			.withCredentials("spring", "password").withStartupAttempts(5).withStartupTimeout(Duration.ofMinutes(10))
			.withBucket(new BucketDefinition(BUCKET_NAME).withPrimaryIndex(false));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CouchbaseAutoConfiguration.class))
			.withPropertyValues("spring.couchbase.connection-string: " + couchbase.getConnectionString(),
					"spring.couchbase.username:spring", "spring.couchbase.password:password",
					"spring.couchbase.bucket.name:" + BUCKET_NAME);

	@Test
	void defaultConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Cluster.class).hasSingleBean(ClusterEnvironment.class);
			Cluster cluster = context.getBean(Cluster.class);
			Bucket bucket = cluster.bucket(BUCKET_NAME);
			bucket.waitUntilReady(Duration.ofMinutes(5));
			DiagnosticsResult diagnostics = cluster.diagnostics();
			assertThat(diagnostics.state()).isEqualTo(ClusterState.ONLINE);
		});
	}

	@Test
	void whenCouchbaseIsUsingCustomObjectMapperThenJsonCanBeRoundTripped() {
		this.contextRunner.withBean(ObjectMapper.class, ObjectMapper::new).run((context) -> {
			Cluster cluster = context.getBean(Cluster.class);
			Bucket bucket = cluster.bucket(BUCKET_NAME);
			bucket.waitUntilReady(Duration.ofMinutes(5));
			Collection collection = bucket.defaultCollection();
			collection.insert("test-document", JsonObject.create().put("a", "alpha"));
			assertThat(collection.get("test-document").contentAsObject().get("a")).isEqualTo("alpha");
		});
	}

}
