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

package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

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
	void cqlSessionBuildHasScopePrototype() {
		this.contextRunner.run((context) -> {
			CqlIdentifier keyspace = CqlIdentifier.fromCql("test");
			CqlSessionBuilder firstBuilder = context.getBean(CqlSessionBuilder.class);
			assertThat(firstBuilder.withKeyspace(keyspace)).hasFieldOrPropertyWithValue("keyspace", keyspace);
			CqlSessionBuilder secondBuilder = context.getBean(CqlSessionBuilder.class);
			assertThat(secondBuilder).hasFieldOrPropertyWithValue("keyspace", null);
		});
	}

	@Test
	void driverConfigLoaderWithDefaultConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(DriverConfigLoader.class);
			assertThat(context.getBean(DriverConfigLoader.class).getInitialConfig().getDefaultProfile()
					.isDefined(DefaultDriverOption.SESSION_NAME)).isFalse();
		});
	}

	@Test
	void driverConfigLoaderWithContactPoints() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.contact-points=cluster.example.com:9042",
				"spring.data.cassandra.local-datacenter=cassandra-eu1").run((context) -> {
					assertThat(context).hasSingleBean(DriverConfigLoader.class);
					DriverExecutionProfile configuration = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(configuration.getStringList(DefaultDriverOption.CONTACT_POINTS))
							.containsOnly("cluster.example.com:9042");
					assertThat(configuration.getString(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER))
							.isEqualTo("cassandra-eu1");
				});
	}

	@Test
	void driverConfigLoaderWithContactPointAndNoPort() {
		this.contextRunner
				.withPropertyValues("spring.data.cassandra.contact-points=cluster.example.com,another.example.com:9041",
						"spring.data.cassandra.local-datacenter=cassandra-eu1")
				.run((context) -> {
					assertThat(context).hasSingleBean(DriverConfigLoader.class);
					DriverExecutionProfile configuration = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(configuration.getStringList(DefaultDriverOption.CONTACT_POINTS))
							.containsOnly("cluster.example.com:9042", "another.example.com:9041");
					assertThat(configuration.getString(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER))
							.isEqualTo("cassandra-eu1");
				});
	}

	@Test
	void driverConfigLoaderWithContactPointAndNoPortAndCustomPort() {
		this.contextRunner
				.withPropertyValues("spring.data.cassandra.contact-points=cluster.example.com:9041,another.example.com",
						"spring.data.cassandra.port=9043", "spring.data.cassandra.local-datacenter=cassandra-eu1")
				.run((context) -> {
					assertThat(context).hasSingleBean(DriverConfigLoader.class);
					DriverExecutionProfile configuration = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(configuration.getStringList(DefaultDriverOption.CONTACT_POINTS))
							.containsOnly("cluster.example.com:9041", "another.example.com:9043");
					assertThat(configuration.getString(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER))
							.isEqualTo("cassandra-eu1");
				});
	}

	@Test
	void driverConfigLoaderWithCustomSessionName() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.session-name=testcluster").run((context) -> {
			assertThat(context).hasSingleBean(DriverConfigLoader.class);
			assertThat(context.getBean(DriverConfigLoader.class).getInitialConfig().getDefaultProfile()
					.getString(DefaultDriverOption.SESSION_NAME)).isEqualTo("testcluster");
		});
	}

	@Test
	void driverConfigLoaderWithCustomSessionNameAndCustomizer() {
		this.contextRunner.withUserConfiguration(SimpleDriverConfigLoaderBuilderCustomizerConfig.class)
				.withPropertyValues("spring.data.cassandra.session-name=testcluster").run((context) -> {
					assertThat(context).hasSingleBean(DriverConfigLoader.class);
					assertThat(context.getBean(DriverConfigLoader.class).getInitialConfig().getDefaultProfile()
							.getString(DefaultDriverOption.SESSION_NAME)).isEqualTo("overridden-name");
				});
	}

	@Test
	void driverConfigLoaderCustomizePoolOptions() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.pool.idle-timeout=42",
				"spring.data.cassandra.pool.heartbeat-interval=62", "spring.data.cassandra.pool.max-queue-size=72")
				.run((context) -> {
					DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(config.getInt(DefaultDriverOption.HEARTBEAT_TIMEOUT)).isEqualTo(42);
					assertThat(config.getInt(DefaultDriverOption.HEARTBEAT_INTERVAL)).isEqualTo(62);
					assertThat(config.getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_QUEUE_SIZE)).isEqualTo(72);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class SimpleDriverConfigLoaderBuilderCustomizerConfig {

		@Bean
		DriverConfigLoaderBuilderCustomizer customizer() {
			return (builder) -> builder.withString(DefaultDriverOption.SESSION_NAME, "overridden-name");
		}

	}

}
