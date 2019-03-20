/*
 * Copyright 2012-2017 the original author or authors.
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
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
public class CassandraAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createClusterWithDefault() {
		load();
		assertThat(this.context.getBeanNamesForType(Cluster.class).length).isEqualTo(1);
		Cluster cluster = this.context.getBean(Cluster.class);
		assertThat(cluster.getClusterName()).startsWith("cluster");
	}

	@Test
	public void createClusterWithOverrides() {
		load("spring.data.cassandra.cluster-name=testcluster");
		assertThat(this.context.getBeanNamesForType(Cluster.class).length).isEqualTo(1);
		Cluster cluster = this.context.getBean(Cluster.class);
		assertThat(cluster.getClusterName()).isEqualTo("testcluster");
	}

	@Test
	public void createCustomizeCluster() {
		load(MockCustomizerConfig.class);
		assertThat(this.context.getBeanNamesForType(Cluster.class).length).isEqualTo(1);
		assertThat(
				this.context.getBeanNamesForType(ClusterBuilderCustomizer.class).length)
						.isEqualTo(1);
	}

	@Test
	public void customizerOverridesAutoConfig() {
		load(SimpleCustomizerConfig.class,
				"spring.data.cassandra.cluster-name=testcluster");
		assertThat(this.context.getBeanNamesForType(Cluster.class).length).isEqualTo(1);
		Cluster cluster = this.context.getBean(Cluster.class);
		assertThat(cluster.getClusterName()).isEqualTo("overridden-name");
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(PropertyPlaceholderAutoConfiguration.class,
				CassandraAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class MockCustomizerConfig {

		@Bean
		public ClusterBuilderCustomizer customizer() {
			return mock(ClusterBuilderCustomizer.class);
		}

	}

	@Configuration
	static class SimpleCustomizerConfig {

		@Bean
		public ClusterBuilderCustomizer customizer() {
			return new ClusterBuilderCustomizer() {
				@Override
				public void customize(Cluster.Builder clusterBuilder) {
					clusterBuilder.withClusterName("overridden-name");
				}
			};
		}

	}

}
