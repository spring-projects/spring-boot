/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.BucketSettings;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.cluster.UserRole;
import com.couchbase.client.java.cluster.UserSettings;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link CouchbaseAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 */
@Testcontainers(disabledWithoutDocker = true)
class CouchbaseAutoConfigurationIntegrationTests {

	@Container
	static final CouchbaseContainer couchbase = new CouchbaseContainer().withClusterAdmin("spring", "password")
			.withStartupAttempts(5).withStartupTimeout(Duration.ofMinutes(10));

	private AnnotationConfigApplicationContext context;

	@BeforeAll
	static void createBucket() {
		BucketSettings bucketSettings = DefaultBucketSettings.builder().enableFlush(true).name("default")
				.password("password").quota(100).replicas(0).type(BucketType.COUCHBASE).build();
		List<UserRole> userSettings = Collections.singletonList(new UserRole("admin"));
		couchbase.createBucket(bucketSettings,
				UserSettings.build().password(bucketSettings.password()).roles(userSettings), true);
	}

	@BeforeEach
	void setUp() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(CouchbaseAutoConfiguration.class);
		TestPropertyValues.of("spring.couchbase.bootstrap-hosts=" + couchbase.getContainerIpAddress(),
				"spring.couchbase.env.bootstrap.http-direct-port:" + couchbase.getMappedPort(8091),
				"spring.couchbase.username:spring", "spring.couchbase.password:password",
				"spring.couchbase.bucket.name:default").applyTo(this.context.getEnvironment());
	}

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void defaultConfiguration() {
		this.context.refresh();
		assertThat(this.context.getBeansOfType(Cluster.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(ClusterInfo.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(CouchbaseEnvironment.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(Bucket.class)).hasSize(1);
	}

	@Test
	void customConfiguration() {
		this.context.register(CustomConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(Cluster.class)).hasSize(2);
		assertThat(this.context.getBeansOfType(ClusterInfo.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(CouchbaseEnvironment.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(Bucket.class)).hasSize(2);
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		Cluster myCustomCouchbaseCluster() {
			return mock(Cluster.class);
		}

		@Bean
		Bucket myCustomCouchbaseClient() {
			return mock(CouchbaseBucket.class);
		}

	}

}
