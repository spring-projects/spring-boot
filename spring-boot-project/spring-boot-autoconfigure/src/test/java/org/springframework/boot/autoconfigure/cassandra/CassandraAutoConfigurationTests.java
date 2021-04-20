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

package org.springframework.boot.autoconfigure.cassandra;

import java.time.Duration;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfig;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.internal.core.session.throttling.ConcurrencyLimitingRequestThrottler;
import com.datastax.oss.driver.internal.core.session.throttling.PassThroughRequestThrottler;
import com.datastax.oss.driver.internal.core.session.throttling.RateLimitingRequestThrottler;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
	void driverConfigLoaderCustomizeConnectionOptions() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.connection.connect-timeout=200ms",
				"spring.data.cassandra.connection.init-query-timeout=10").run((context) -> {
					DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(config.getInt(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT)).isEqualTo(200);
					assertThat(config.getInt(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT)).isEqualTo(10);
				});
	}

	@Test
	void driverConfigLoaderCustomizePoolOptions() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.pool.idle-timeout=42",
				"spring.data.cassandra.pool.heartbeat-interval=62").run((context) -> {
					DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(config.getInt(DefaultDriverOption.HEARTBEAT_TIMEOUT)).isEqualTo(42);
					assertThat(config.getInt(DefaultDriverOption.HEARTBEAT_INTERVAL)).isEqualTo(62);
				});
	}

	@Test
	void driverConfigLoaderCustomizeRequestOptions() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.request.timeout=5s",
				"spring.data.cassandra.request.consistency=two",
				"spring.data.cassandra.request.serial-consistency=quorum", "spring.data.cassandra.request.page-size=42")
				.run((context) -> {
					DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(config.getInt(DefaultDriverOption.REQUEST_TIMEOUT)).isEqualTo(5000);
					assertThat(config.getString(DefaultDriverOption.REQUEST_CONSISTENCY)).isEqualTo("TWO");
					assertThat(config.getString(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY)).isEqualTo("QUORUM");
					assertThat(config.getInt(DefaultDriverOption.REQUEST_PAGE_SIZE)).isEqualTo(42);
				});
	}

	@Test
	void driverConfigLoaderCustomizeControlConnectionOptions() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.controlconnection.timeout=200ms")
				.run((context) -> {
					DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(config.getInt(DefaultDriverOption.CONTROL_CONNECTION_TIMEOUT)).isEqualTo(200);
				});
	}

	@Test
	void driverConfigLoaderUsePassThroughLimitingRequestThrottlerByDefault() {
		this.contextRunner.withPropertyValues().run((context) -> {
			DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
					.getDefaultProfile();
			assertThat(config.getString(DefaultDriverOption.REQUEST_THROTTLER_CLASS))
					.isEqualTo(PassThroughRequestThrottler.class.getSimpleName());
		});
	}

	@Test
	void driverConfigLoaderWithRateLimitingRequiresExtraConfiguration() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.request.throttler.type=rate-limiting")
				.run((context) -> assertThatThrownBy(() -> context.getBean(CqlSession.class))
						.hasMessageContaining("Error instantiating class RateLimitingRequestThrottler")
						.hasMessageContaining("No configuration setting found for key"));
	}

	@Test
	void driverConfigLoaderCustomizeConcurrencyLimitingRequestThrottler() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.request.throttler.type=concurrency-limiting",
				"spring.data.cassandra.request.throttler.max-concurrent-requests=62",
				"spring.data.cassandra.request.throttler.max-queue-size=72").run((context) -> {
					DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(config.getString(DefaultDriverOption.REQUEST_THROTTLER_CLASS))
							.isEqualTo(ConcurrencyLimitingRequestThrottler.class.getSimpleName());
					assertThat(config.getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_CONCURRENT_REQUESTS))
							.isEqualTo(62);
					assertThat(config.getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_QUEUE_SIZE)).isEqualTo(72);
				});
	}

	@Test
	void driverConfigLoaderCustomizeRateLimitingRequestThrottler() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.request.throttler.type=rate-limiting",
				"spring.data.cassandra.request.throttler.max-requests-per-second=62",
				"spring.data.cassandra.request.throttler.max-queue-size=72",
				"spring.data.cassandra.request.throttler.drain-interval=16ms").run((context) -> {
					DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(config.getString(DefaultDriverOption.REQUEST_THROTTLER_CLASS))
							.isEqualTo(RateLimitingRequestThrottler.class.getSimpleName());
					assertThat(config.getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_REQUESTS_PER_SECOND))
							.isEqualTo(62);
					assertThat(config.getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_QUEUE_SIZE)).isEqualTo(72);
					assertThat(config.getInt(DefaultDriverOption.REQUEST_THROTTLER_DRAIN_INTERVAL)).isEqualTo(16);
				});
	}

	@Test
	void driverConfigLoaderWithConfigComplementSettings() {
		String configLocation = "org/springframework/boot/autoconfigure/cassandra/simple.conf";
		this.contextRunner.withPropertyValues("spring.data.cassandra.session-name=testcluster",
				"spring.data.cassandra.config=" + configLocation).run((context) -> {
					assertThat(context).hasSingleBean(DriverConfigLoader.class);
					assertThat(context.getBean(DriverConfigLoader.class).getInitialConfig().getDefaultProfile()
							.getString(DefaultDriverOption.SESSION_NAME)).isEqualTo("testcluster");
					assertThat(context.getBean(DriverConfigLoader.class).getInitialConfig().getDefaultProfile()
							.getDuration(DefaultDriverOption.REQUEST_TIMEOUT)).isEqualTo(Duration.ofMillis(500));
				});
	}

	@Test
	void driverConfigLoaderWithConfigCreateProfiles() {
		String configLocation = "org/springframework/boot/autoconfigure/cassandra/profiles.conf";
		this.contextRunner.withPropertyValues("spring.data.cassandra.config=" + configLocation).run((context) -> {
			assertThat(context).hasSingleBean(DriverConfigLoader.class);
			DriverConfig driverConfig = context.getBean(DriverConfigLoader.class).getInitialConfig();
			assertThat(driverConfig.getProfiles()).containsOnlyKeys("default", "first", "second");
			assertThat(driverConfig.getProfile("first").getDuration(DefaultDriverOption.REQUEST_TIMEOUT))
					.isEqualTo(Duration.ofMillis(100));
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
