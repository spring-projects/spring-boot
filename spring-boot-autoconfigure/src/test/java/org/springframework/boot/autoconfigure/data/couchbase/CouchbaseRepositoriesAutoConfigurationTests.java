/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.couchbase;

import java.util.Collections;
import java.util.List;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterInfo;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.couchbase.city.City;
import org.springframework.boot.autoconfigure.data.couchbase.city.CityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.WriteResultChecking;
import org.springframework.data.couchbase.core.query.Consistency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Eddú Meléndez
 */
public class CouchbaseRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		addCouchbaseProperties(this.context);
		this.context.register(TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
		assertThat(this.context.getBean(Bucket.class)).isNotNull();
	}

	@Test
	public void testNoRepositoryConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		addCouchbaseProperties(this.context);
		this.context.register(EmptyConfiguration.class, TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(Bucket.class)).isNotNull();
	}

	@Test
	public void templateExists() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.couchbase.hosts=localhost",
				"spring.data.couchbase.bucket-name=test",
				"spring.data.couchbase.bucket-password=test");
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(CouchbaseTemplate.class).length).isEqualTo(1);
	}

	private void addCouchbaseProperties(AnnotationConfigApplicationContext context) {
		EnvironmentTestUtils.addEnvironment(context,
				"spring.data.couchbase.hosts=localhost",
				"spring.data.couchbase.bucket-name=test",
				"spring.data.couchbase.bucket-password=test");
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	@Import(CouchbaseRepositoriesRegistrar.class)
	static class TestConfiguration extends AbstractCouchbaseConfiguration {

		@Override
		protected List<String> getBootstrapHosts() {
			return Collections.singletonList("192.1.2.3");
		}

		@Override
		protected String getBucketName() {
			return "someBucket";
		}

		@Override
		protected String getBucketPassword() {
			return "someBucketPassword";
		}

		@Override
		public Cluster couchbaseCluster() throws Exception {
			return mock(CouchbaseCluster.class);
		}

		@Bean
		public ClusterInfo couchbaseClusterInfo() {
			return mock(ClusterInfo.class);
		}

		@Override
		public Bucket couchbaseClient() throws Exception {
			return mock(CouchbaseBucket.class);
		}

		@Override
		public CouchbaseTemplate couchbaseTemplate() throws Exception {
			CouchbaseTemplate template = super.couchbaseTemplate();
			template.setWriteResultChecking(WriteResultChecking.LOG);
			return template;
		}

		@Override
		protected Consistency getDefaultConsistency() {
			return Consistency.READ_YOUR_OWN_WRITES;
		}

	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	protected static class EmptyConfiguration {

	}

}
