/*
 * Copyright 2012-2016 the original author or authors.
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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration.CouchbaseConfiguration;
import org.springframework.boot.autoconfigure.data.couchbase.CouchbaseDataAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CouchbaseAutoConfiguration}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
public class CouchbaseAutoConfigurationTests
		extends AbstractCouchbaseAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void bootstrapHostsIsRequired() {
		load(null);
		assertNoCouchbaseBeans();
	}

	@Test
	public void bootstrapHostsNotRequiredIfCouchbaseConfigurerIsSet() {
		load(CouchbaseTestConfigurer.class);
		assertThat(this.context.getBeansOfType(CouchbaseTestConfigurer.class)).hasSize(1);
		// No beans are going to be created
		assertNoCouchbaseBeans();
	}

	@Test
	public void bootstrapHostsIgnoredIfCouchbaseConfigurerIsSet() {
		load(CouchbaseTestConfigurer.class, "spring.couchbase.bootstrapHosts=localhost");
		assertThat(this.context.getBeansOfType(CouchbaseTestConfigurer.class)).hasSize(1);
		assertNoCouchbaseBeans();
	}

	private void assertNoCouchbaseBeans() {
		// No beans are going to be created
		assertThat(this.context.getBeansOfType(CouchbaseEnvironment.class)).isEmpty();
		assertThat(this.context.getBeansOfType(ClusterInfo.class)).isEmpty();
		assertThat(this.context.getBeansOfType(Cluster.class)).isEmpty();
		assertThat(this.context.getBeansOfType(Bucket.class)).isEmpty();
	}

	@Test
	public void customizeEnvEndpoints() throws Exception {
		DefaultCouchbaseEnvironment env = customizeEnv(
				"spring.couchbase.env.endpoints.keyValue=4",
				"spring.couchbase.env.endpoints.query=5",
				"spring.couchbase.env.endpoints.view=6");
		assertThat(env.kvEndpoints()).isEqualTo(4);
		assertThat(env.queryEndpoints()).isEqualTo(5);
		assertThat(env.viewEndpoints()).isEqualTo(6);
	}

	@Test
	public void customizeEnvTimeouts() throws Exception {
		DefaultCouchbaseEnvironment env = customizeEnv(
				"spring.couchbase.env.timeouts.connect=100",
				"spring.couchbase.env.timeouts.keyValue=200",
				"spring.couchbase.env.timeouts.query=300",
				"spring.couchbase.env.timeouts.socket-connect=400",
				"spring.couchbase.env.timeouts.view=500");
		assertThat(env.connectTimeout()).isEqualTo(100);
		assertThat(env.kvTimeout()).isEqualTo(200);
		assertThat(env.queryTimeout()).isEqualTo(300);
		assertThat(env.socketConnectTimeout()).isEqualTo(400);
		assertThat(env.viewTimeout()).isEqualTo(500);
	}

	@Test
	public void enableSslNoEnabledFlag() throws Exception {
		DefaultCouchbaseEnvironment env = customizeEnv(
				"spring.couchbase.env.ssl.keyStore=foo",
				"spring.couchbase.env.ssl.keyStorePassword=secret");
		assertThat(env.sslEnabled()).isTrue();
		assertThat(env.sslKeystoreFile()).isEqualTo("foo");
		assertThat(env.sslKeystorePassword()).isEqualTo("secret");
	}

	@Test
	public void disableSslEvenWithKeyStore() throws Exception {
		DefaultCouchbaseEnvironment env = customizeEnv(
				"spring.couchbase.env.ssl.enabled=false",
				"spring.couchbase.env.ssl.keyStore=foo",
				"spring.couchbase.env.ssl.keyStorePassword=secret");
		assertThat(env.sslEnabled()).isFalse();
		assertThat(env.sslKeystoreFile()).isNull();
		assertThat(env.sslKeystorePassword()).isNull();
	}

	@Test
	public void customizeEnvWithCustomCouchbaseConfiguration() {
		load(CustomCouchbaseConfiguration.class,
				"spring.couchbase.bootstrap-hosts=localhost",
				"spring.couchbase.env.timeouts.connect=100");
		assertThat(this.context.getBeansOfType(CouchbaseConfiguration.class)).hasSize(1);
		DefaultCouchbaseEnvironment env = this.context
				.getBean(DefaultCouchbaseEnvironment.class);
		assertThat(env.socketConnectTimeout()).isEqualTo(5000);
		assertThat(env.connectTimeout()).isEqualTo(2000);
	}

	private DefaultCouchbaseEnvironment customizeEnv(String... environment)
			throws Exception {
		load(CouchbaseTestConfigurer.class, environment);
		CouchbaseProperties properties = this.context.getBean(CouchbaseProperties.class);
		return new CouchbaseConfiguration(properties).couchbaseEnvironment();
	}

	@Configuration
	@Import(CouchbaseDataAutoConfiguration.class)
	static class CustomCouchbaseConfiguration extends CouchbaseConfiguration {

		CustomCouchbaseConfiguration(CouchbaseProperties properties) {
			super(properties);
		}

		@Override
		protected DefaultCouchbaseEnvironment.Builder initializeEnvironmentBuilder(
				CouchbaseProperties properties) {
			return super.initializeEnvironmentBuilder(properties)
					.socketConnectTimeout(5000).connectTimeout(2000);
		}

		@Override
		public Cluster couchbaseCluster() throws Exception {
			return mock(Cluster.class);
		}

		@Override
		public ClusterInfo couchbaseClusterInfo() {
			return mock(ClusterInfo.class);
		}

		@Override
		public Bucket couchbaseClient() {
			return mock(CouchbaseBucket.class);
		}

	}

}
