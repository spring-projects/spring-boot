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

import java.util.function.Consumer;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
class CouchbaseAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CouchbaseAutoConfiguration.class));

	@Test
	void bootstrapHostsIsRequired() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(CouchbaseEnvironment.class)
				.doesNotHaveBean(Cluster.class));
	}

	@Test
	void bootstrapHostsCreateEnvironmentAndCluster() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfiguration.class)
				.withPropertyValues("spring.couchbase.bootstrap-hosts=localhost").run((context) -> {
					assertThat(context).hasSingleBean(CouchbaseEnvironment.class).hasSingleBean(Cluster.class);
					assertThat(context.getBean(Cluster.class))
							.isSameAs(context.getBean(CouchbaseTestConfiguration.class).couchbaseCluster());
				});
	}

	@Test
	void customizeEnvEndpoints() {
		testCouchbaseEnv((env) -> {
			assertThat(env.kvServiceConfig().minEndpoints()).isEqualTo(2);
			assertThat(env.kvServiceConfig().maxEndpoints()).isEqualTo(2);
			assertThat(env.queryServiceConfig().minEndpoints()).isEqualTo(3);
			assertThat(env.queryServiceConfig().maxEndpoints()).isEqualTo(5);
			assertThat(env.viewServiceConfig().minEndpoints()).isEqualTo(4);
			assertThat(env.viewServiceConfig().maxEndpoints()).isEqualTo(6);
		}, "spring.couchbase.env.endpoints.key-value=2", "spring.couchbase.env.endpoints.queryservice.min-endpoints=3",
				"spring.couchbase.env.endpoints.queryservice.max-endpoints=5",
				"spring.couchbase.env.endpoints.viewservice.min-endpoints=4",
				"spring.couchbase.env.endpoints.viewservice.max-endpoints=6");
	}

	@Test
	void customizeEnvEndpointsUsesNewInfrastructure() {
		testCouchbaseEnv((env) -> {
			assertThat(env.queryServiceConfig().minEndpoints()).isEqualTo(3);
			assertThat(env.queryServiceConfig().maxEndpoints()).isEqualTo(5);
			assertThat(env.viewServiceConfig().minEndpoints()).isEqualTo(4);
			assertThat(env.viewServiceConfig().maxEndpoints()).isEqualTo(6);
		}, "spring.couchbase.env.endpoints.queryservice.min-endpoints=3",
				"spring.couchbase.env.endpoints.queryservice.max-endpoints=5",
				"spring.couchbase.env.endpoints.viewservice.min-endpoints=4",
				"spring.couchbase.env.endpoints.viewservice.max-endpoints=6");
	}

	@Test
	void customizeEnvEndpointsUsesNewInfrastructureWithOnlyMax() {
		testCouchbaseEnv((env) -> {
			assertThat(env.queryServiceConfig().minEndpoints()).isEqualTo(1);
			assertThat(env.queryServiceConfig().maxEndpoints()).isEqualTo(5);
			assertThat(env.viewServiceConfig().minEndpoints()).isEqualTo(1);
			assertThat(env.viewServiceConfig().maxEndpoints()).isEqualTo(6);
		}, "spring.couchbase.env.endpoints.queryservice.max-endpoints=5",
				"spring.couchbase.env.endpoints.viewservice.max-endpoints=6");
	}

	@Test
	void customizeEnvTimeouts() {
		testCouchbaseEnv((env) -> {
			assertThat(env.connectTimeout()).isEqualTo(100);
			assertThat(env.kvTimeout()).isEqualTo(200);
			assertThat(env.queryTimeout()).isEqualTo(300);
			assertThat(env.socketConnectTimeout()).isEqualTo(400);
			assertThat(env.viewTimeout()).isEqualTo(500);
		}, "spring.couchbase.env.timeouts.connect=100", "spring.couchbase.env.timeouts.keyValue=200",
				"spring.couchbase.env.timeouts.query=300", "spring.couchbase.env.timeouts.socket-connect=400",
				"spring.couchbase.env.timeouts.view=500");
	}

	@Test
	void enableSslNoEnabledFlag() {
		testCouchbaseEnv((env) -> {
			assertThat(env.sslEnabled()).isTrue();
			assertThat(env.sslKeystoreFile()).isEqualTo("foo");
			assertThat(env.sslKeystorePassword()).isEqualTo("secret");
		}, "spring.couchbase.env.ssl.keyStore=foo", "spring.couchbase.env.ssl.keyStorePassword=secret");
	}

	@Test
	void disableSslEvenWithKeyStore() {
		testCouchbaseEnv((env) -> {
			assertThat(env.sslEnabled()).isFalse();
			assertThat(env.sslKeystoreFile()).isNull();
			assertThat(env.sslKeystorePassword()).isNull();
		}, "spring.couchbase.env.ssl.enabled=false", "spring.couchbase.env.ssl.keyStore=foo",
				"spring.couchbase.env.ssl.keyStorePassword=secret");
	}

	private void testCouchbaseEnv(Consumer<DefaultCouchbaseEnvironment> environmentConsumer, String... environment) {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfiguration.class)
				.withPropertyValues("spring.couchbase.bootstrap-hosts=localhost").withPropertyValues(environment)
				.run((context) -> environmentConsumer.accept(context.getBean(DefaultCouchbaseEnvironment.class)));
	}

	@Test
	void customizeEnvWithCustomCouchbaseConfiguration() {
		this.contextRunner
				.withUserConfiguration(CouchbaseTestConfiguration.class,
						CouchbaseEnvironmentCustomizerConfiguration.class)
				.withPropertyValues("spring.couchbase.bootstrap-hosts=localhost",
						"spring.couchbase.env.timeouts.connect=100")
				.run((context) -> {
					assertThat(context).hasSingleBean(DefaultCouchbaseEnvironment.class);
					DefaultCouchbaseEnvironment env = context.getBean(DefaultCouchbaseEnvironment.class);
					assertThat(env.socketConnectTimeout()).isEqualTo(5000);
					assertThat(env.connectTimeout()).isEqualTo(2000);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class CouchbaseEnvironmentCustomizerConfiguration {

		@Bean
		CouchbaseEnvironmentBuilderCustomizer couchbaseEnvironmentBuilderCustomizer() {
			return (builder) -> builder.socketConnectTimeout(5000).connectTimeout(2000);
		}

	}

}
