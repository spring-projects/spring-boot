/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Initializer;
import com.datastax.driver.core.PoolingOptions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraAutoConfiguration}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
class CassandraAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CassandraAutoConfiguration.class));

	@Test
	void createClusterWithDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Cluster.class);
			assertThat(context.getBean(Cluster.class).getClusterName()).startsWith("cluster");
		});
	}

	@Test
	void createClusterWithOverrides() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.cluster-name=testcluster").run((context) -> {
			assertThat(context).hasSingleBean(Cluster.class);
			assertThat(context.getBean(Cluster.class).getClusterName()).isEqualTo("testcluster");
		});
	}

	@Test
	void createCustomizeCluster() {
		this.contextRunner.withUserConfiguration(MockCustomizerConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(Cluster.class);
			assertThat(context).hasSingleBean(ClusterBuilderCustomizer.class);
		});
	}

	@Test
	void customizerOverridesAutoConfig() {
		this.contextRunner.withUserConfiguration(SimpleCustomizerConfig.class)
				.withPropertyValues("spring.data.cassandra.cluster-name=testcluster").run((context) -> {
					assertThat(context).hasSingleBean(Cluster.class);
					assertThat(context.getBean(Cluster.class).getClusterName()).isEqualTo("overridden-name");
				});
	}

	@Test
	void defaultPoolOptions() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Cluster.class);
			PoolingOptions poolingOptions = context.getBean(Cluster.class).getConfiguration().getPoolingOptions();
			assertThat(poolingOptions.getIdleTimeoutSeconds()).isEqualTo(PoolingOptions.DEFAULT_IDLE_TIMEOUT_SECONDS);
			assertThat(poolingOptions.getPoolTimeoutMillis()).isEqualTo(PoolingOptions.DEFAULT_POOL_TIMEOUT_MILLIS);
			assertThat(poolingOptions.getHeartbeatIntervalSeconds())
					.isEqualTo(PoolingOptions.DEFAULT_HEARTBEAT_INTERVAL_SECONDS);
			assertThat(poolingOptions.getMaxQueueSize()).isEqualTo(PoolingOptions.DEFAULT_MAX_QUEUE_SIZE);
		});
	}

	@Test
	void customizePoolOptions() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.pool.idle-timeout=42",
				"spring.data.cassandra.pool.pool-timeout=52", "spring.data.cassandra.pool.heartbeat-interval=62",
				"spring.data.cassandra.pool.max-queue-size=72").run((context) -> {
					assertThat(context).hasSingleBean(Cluster.class);
					PoolingOptions poolingOptions = context.getBean(Cluster.class).getConfiguration()
							.getPoolingOptions();
					assertThat(poolingOptions.getIdleTimeoutSeconds()).isEqualTo(42);
					assertThat(poolingOptions.getPoolTimeoutMillis()).isEqualTo(52);
					assertThat(poolingOptions.getHeartbeatIntervalSeconds()).isEqualTo(62);
					assertThat(poolingOptions.getMaxQueueSize()).isEqualTo(72);
				});
	}

	@Test
	void clusterFactoryIsCalledToCreateCluster() {
		this.contextRunner.withUserConfiguration(ClusterFactoryConfig.class)
				.run((context) -> assertThat(context.getBean(TestClusterFactory.class).initializer).isNotNull());
	}

	@Configuration(proxyBeanMethods = false)
	static class MockCustomizerConfig {

		@Bean
		ClusterBuilderCustomizer customizer() {
			return mock(ClusterBuilderCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SimpleCustomizerConfig {

		@Bean
		ClusterBuilderCustomizer customizer() {
			return (clusterBuilder) -> clusterBuilder.withClusterName("overridden-name");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClusterFactoryConfig {

		@Bean
		TestClusterFactory clusterFactory() {
			return new TestClusterFactory();
		}

	}

	static class TestClusterFactory implements ClusterFactory {

		private Initializer initializer = null;

		@Override
		public Cluster create(Initializer initializer) {
			this.initializer = initializer;
			return Cluster.buildFrom(initializer);
		}

	}

}
