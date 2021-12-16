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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.couchbase.client.core.env.IoConfig;
import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.codec.JacksonJsonSerializer;
import com.couchbase.client.java.codec.JsonSerializer;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonValueModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
	void connectionStringIsRequired() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ClusterEnvironment.class)
				.doesNotHaveBean(Cluster.class));
	}

	@Test
	void connectionStringCreateEnvironmentAndCluster() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfiguration.class)
				.withPropertyValues("spring.couchbase.connection-string=localhost").run((context) -> {
					assertThat(context).hasSingleBean(ClusterEnvironment.class).hasSingleBean(Cluster.class);
					assertThat(context.getBean(Cluster.class))
							.isSameAs(context.getBean(CouchbaseTestConfiguration.class).couchbaseCluster());
				});
	}

	@Test
	void whenObjectMapperBeanIsDefinedThenClusterEnvironmentObjectMapperIsDerivedFromIt() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfiguration.class)
				.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
				.withPropertyValues("spring.couchbase.connection-string=localhost").run((context) -> {
					ClusterEnvironment env = context.getBean(ClusterEnvironment.class);
					Set<Object> expectedModuleIds = new HashSet<>(
							context.getBean(ObjectMapper.class).getRegisteredModuleIds());
					expectedModuleIds.add(new JsonValueModule().getTypeId());
					JsonSerializer serializer = env.jsonSerializer();
					assertThat(serializer).extracting("wrapped").isInstanceOf(JacksonJsonSerializer.class)
							.extracting("mapper", as(InstanceOfAssertFactories.type(ObjectMapper.class)))
							.extracting(ObjectMapper::getRegisteredModuleIds).isEqualTo(expectedModuleIds);
				});
	}

	@Test
	void customizeJsonSerializer() {
		JsonSerializer customJsonSerializer = mock(JsonSerializer.class);
		this.contextRunner.withUserConfiguration(CouchbaseTestConfiguration.class)
				.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
				.withBean(ClusterEnvironmentBuilderCustomizer.class,
						() -> (builder) -> builder.jsonSerializer(customJsonSerializer))
				.withPropertyValues("spring.couchbase.connection-string=localhost").run((context) -> {
					ClusterEnvironment env = context.getBean(ClusterEnvironment.class);
					JsonSerializer serializer = env.jsonSerializer();
					assertThat(serializer).extracting("wrapped").isSameAs(customJsonSerializer);
				});
	}

	@Test
	void customizeEnvIo() {
		testClusterEnvironment((env) -> {
			IoConfig ioConfig = env.ioConfig();
			assertThat(ioConfig.numKvConnections()).isEqualTo(2);
			assertThat(ioConfig.maxHttpConnections()).isEqualTo(5);
			assertThat(ioConfig.idleHttpConnectionTimeout()).isEqualTo(Duration.ofSeconds(3));
		}, "spring.couchbase.env.io.min-endpoints=2", "spring.couchbase.env.io.max-endpoints=5",
				"spring.couchbase.env.io.idle-http-connection-timeout=3s");
	}

	@Test
	void customizeEnvTimeouts() {
		testClusterEnvironment((env) -> {
			TimeoutConfig timeoutConfig = env.timeoutConfig();
			assertThat(timeoutConfig.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
			assertThat(timeoutConfig.disconnectTimeout()).isEqualTo(Duration.ofSeconds(2));
			assertThat(timeoutConfig.kvTimeout()).isEqualTo(Duration.ofMillis(500));
			assertThat(timeoutConfig.kvDurableTimeout()).isEqualTo(Duration.ofMillis(750));
			assertThat(timeoutConfig.queryTimeout()).isEqualTo(Duration.ofSeconds(3));
			assertThat(timeoutConfig.viewTimeout()).isEqualTo(Duration.ofSeconds(4));
			assertThat(timeoutConfig.searchTimeout()).isEqualTo(Duration.ofSeconds(5));
			assertThat(timeoutConfig.analyticsTimeout()).isEqualTo(Duration.ofSeconds(6));
			assertThat(timeoutConfig.managementTimeout()).isEqualTo(Duration.ofSeconds(7));
		}, "spring.couchbase.env.timeouts.connect=1s", "spring.couchbase.env.timeouts.disconnect=2s",
				"spring.couchbase.env.timeouts.key-value=500ms",
				"spring.couchbase.env.timeouts.key-value-durable=750ms", "spring.couchbase.env.timeouts.query=3s",
				"spring.couchbase.env.timeouts.view=4s", "spring.couchbase.env.timeouts.search=5s",
				"spring.couchbase.env.timeouts.analytics=6s", "spring.couchbase.env.timeouts.management=7s");
	}

	@Test
	void enableSslNoEnabledFlag() {
		testClusterEnvironment((env) -> {
			SecurityConfig securityConfig = env.securityConfig();
			assertThat(securityConfig.tlsEnabled()).isTrue();
			assertThat(securityConfig.trustManagerFactory()).isNotNull();
		}, "spring.couchbase.env.ssl.keyStore=classpath:test.jks", "spring.couchbase.env.ssl.keyStorePassword=secret");
	}

	@Test
	void disableSslEvenWithKeyStore() {
		testClusterEnvironment((env) -> {
			SecurityConfig securityConfig = env.securityConfig();
			assertThat(securityConfig.tlsEnabled()).isFalse();
			assertThat(securityConfig.trustManagerFactory()).isNull();
		}, "spring.couchbase.env.ssl.enabled=false", "spring.couchbase.env.ssl.keyStore=classpath:test.jks",
				"spring.couchbase.env.ssl.keyStorePassword=secret");
	}

	private void testClusterEnvironment(Consumer<ClusterEnvironment> environmentConsumer, String... environment) {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfiguration.class)
				.withPropertyValues("spring.couchbase.connection-string=localhost").withPropertyValues(environment)
				.run((context) -> environmentConsumer.accept(context.getBean(ClusterEnvironment.class)));
	}

	@Test
	void customizeEnvWithCustomCouchbaseConfiguration() {
		this.contextRunner
				.withUserConfiguration(CouchbaseTestConfiguration.class,
						ClusterEnvironmentCustomizerConfiguration.class)
				.withPropertyValues("spring.couchbase.connection-string=localhost",
						"spring.couchbase.env.timeouts.connect=100")
				.run((context) -> {
					assertThat(context).hasSingleBean(ClusterEnvironment.class);
					ClusterEnvironment env = context.getBean(ClusterEnvironment.class);
					assertThat(env.timeoutConfig().kvTimeout()).isEqualTo(Duration.ofSeconds(5));
					assertThat(env.timeoutConfig().connectTimeout()).isEqualTo(Duration.ofSeconds(2));
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class ClusterEnvironmentCustomizerConfiguration {

		@Bean
		ClusterEnvironmentBuilderCustomizer clusterEnvironmentBuilderCustomizer() {
			return (builder) -> builder.timeoutConfig().kvTimeout(Duration.ofSeconds(5))
					.connectTimeout(Duration.ofSeconds(2));
		}

	}

}
