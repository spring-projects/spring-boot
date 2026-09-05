/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.r2dbc.autoconfigure.metrics;

import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.h2.CloseableConnectionFactory;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.h2.H2ConnectionOption;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.pool.PoolMetricsRecorder;
import reactor.pool.introspection.micrometer.Micrometer;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.r2dbc.ConnectionFactoryDecorator;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionPoolMetricsAutoConfiguration}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @author Goutam Adwant
 */
class ConnectionPoolMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.r2dbc.generate-unique-name=true", "management.metrics.use-global-registry=false")
		.withBean(SimpleMeterRegistry.class)
		.withConfiguration(
				AutoConfigurations.of(ConnectionPoolMetricsAutoConfiguration.class, MetricsAutoConfiguration.class));

	@Test
	void autoConfiguredPoolRecordsAllocation() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class)).run((context) -> {
			Connection connection = Mono.from(context.getBean(ConnectionFactory.class).create()).block();
			assertThat(connection).isNotNull();
			Mono.from(connection.close()).block();
			assertThat(context.getBean(MeterRegistry.class)
				.get("reactor.pool.allocation")
				.tags("pool.name", "connectionFactory", "pool.allocation.outcome", "success")
				.timer()
				.count()).isPositive();
		});
	}

	@Test
	void autoConfiguredDataSourceIsInstrumented() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class)).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find("r2dbc.pool.acquired").gauges()).hasSize(1);
		});
	}

	@Test
	void autoConfiguredDataSourceExposedAsConnectionFactoryTypeIsInstrumented() {
		this.contextRunner
			.withPropertyValues(
					"spring.r2dbc.url:r2dbc:pool:h2:mem:///name?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("r2dbc.pool.acquired").gauges()).hasSize(1);
			});
	}

	@Test
	void connectionPoolInstrumentationCanBeDisabled() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withPropertyValues("management.metrics.enable.r2dbc=false")
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("r2dbc.pool.acquired").gauge()).isNull();
			});
	}

	@Test
	void connectionPoolExposedAsConnectionFactoryTypeIsInstrumented() {
		this.contextRunner.withUserConfiguration(ConnectionFactoryConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find("r2dbc.pool.acquired").gauges()).extracting(Meter::getId)
				.extracting((id) -> id.getTag("name"))
				.containsExactly("testConnectionPool");
		});
	}

	@Test
	void wrappedConnectionPoolExposedAsConnectionFactoryTypeIsInstrumented() {
		this.contextRunner.withUserConfiguration(WrappedConnectionPoolConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find("r2dbc.pool.acquired").gauges()).extracting(Meter::getId)
				.extracting((id) -> id.getTag("name"))
				.containsExactly("wrappedConnectionPool");
		});
	}

	@Test
	void allConnectionPoolsCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(MultipleConnectionPoolsConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find("r2dbc.pool.acquired").meters()).map((meter) -> meter.getId().getTag("name"))
				.containsOnly("standardPool", "nonDefaultPool");
		});
	}

	@Test
	void recorderDependencyIsOptional() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Micrometer.class))
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed().doesNotHaveBean(ConnectionPoolMetricsRecorder.class);
				useConnection(context.getBean(ConnectionFactory.class));
				assertThat(context.getBean(MeterRegistry.class).find("r2dbc.pool.acquired").gauge()).isNotNull();
				assertThat(context.getBean(MeterRegistry.class).find("reactor.pool.allocation").timer()).isNull();
			});
	}

	@Test
	void urlConfiguredPoolHasOnlyGauges() {
		this.contextRunner.withPropertyValues("spring.r2dbc.url=r2dbc:pool:h2:mem:///urlPool")
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				useConnection(context.getBean(ConnectionFactory.class));
				assertThat(context.getBean(MeterRegistry.class).find("r2dbc.pool.acquired").gauge()).isNotNull();
				assertThat(context.getBean(MeterRegistry.class).find("reactor.pool.allocation").timer()).isNull();
			});
	}

	@Test
	void manuallyConfiguredPoolHasOnlyGauges() {
		this.contextRunner.withUserConfiguration(ConnectionFactoryConfiguration.class).run((context) -> {
			useConnection(context.getBean(ConnectionFactory.class));
			assertThat(context.getBean(MeterRegistry.class).find("r2dbc.pool.acquired").gauge()).isNotNull();
			assertThat(context.getBean(MeterRegistry.class).find("reactor.pool.allocation").timer()).isNull();
		});
	}

	@Test
	void customRecorderReplacesDefault() {
		PoolMetricsRecorder recorder = mock(PoolMetricsRecorder.class);
		this.contextRunner.withBean("r2dbcPoolMetricsRecorder", PoolMetricsRecorder.class, () -> recorder)
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ConnectionPoolMetricsRecorder.class);
				useConnection(context.getBean(ConnectionFactory.class));
				then(recorder).should(atLeastOnce()).recordAllocationSuccessAndLatency(anyLong());
				assertThat(context.getBean(MeterRegistry.class).find("reactor.pool.allocation").timer()).isNull();
			});
	}

	@Test
	void unrelatedUniqueRecorderDoesNotReplaceDefault() {
		PoolMetricsRecorder recorder = mock(PoolMetricsRecorder.class);
		this.contextRunner.withBean("otherPoolRecorder", PoolMetricsRecorder.class, () -> recorder)
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionPoolMetricsRecorder.class);
				useConnection(context.getBean(ConnectionFactory.class));
				then(recorder).shouldHaveNoInteractions();
				assertThat(context.getBean(MeterRegistry.class)
					.get("reactor.pool.allocation")
					.tags("pool.allocation.outcome", "success")
					.timer()
					.count()).isPositive();
			});
	}

	@Test
	void unrelatedRecordersDoNotReplaceDefault() {
		PoolMetricsRecorder first = mock(PoolMetricsRecorder.class);
		PoolMetricsRecorder second = mock(PoolMetricsRecorder.class);
		this.contextRunner.withBean("firstRecorder", PoolMetricsRecorder.class, () -> first)
			.withBean("secondRecorder", PoolMetricsRecorder.class, () -> second)
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed().hasSingleBean(ConnectionPoolMetricsRecorder.class);
				useConnection(context.getBean(ConnectionFactory.class));
				then(first).shouldHaveNoInteractions();
				then(second).shouldHaveNoInteractions();
			});
	}

	@Test
	void unrelatedPrimaryRecorderDoesNotReplaceDefault() {
		PoolMetricsRecorder first = mock(PoolMetricsRecorder.class);
		PoolMetricsRecorder second = mock(PoolMetricsRecorder.class);
		this.contextRunner.withBean("firstRecorder", PoolMetricsRecorder.class, () -> first)
			.withBean("secondRecorder", PoolMetricsRecorder.class, () -> second,
					(definition) -> definition.setPrimary(true))
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				useConnection(context.getBean(ConnectionFactory.class));
				assertThat(context).hasSingleBean(ConnectionPoolMetricsRecorder.class);
				then(first).shouldHaveNoInteractions();
				then(second).shouldHaveNoInteractions();
			});
	}

	@Test
	void registryCustomizerCanUseConnectionFactory() {
		this.contextRunner.withUserConfiguration(RegistryCustomizerConfiguration.class)
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				useConnection(context.getBean(ConnectionFactory.class));
				assertThat(context.getBean(MeterRegistry.class)
					.get("reactor.pool.allocation")
					.tags("pool.allocation.outcome", "success", "database", "h2")
					.timer()
					.count()).isPositive();
			});
	}

	@Test
	void lifecycleMetricsCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.reactor.pool=false")
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				useConnection(context.getBean(ConnectionFactory.class));
				assertThat(context.getBean(MeterRegistry.class).find("reactor.pool.allocation").timer()).isNull();
				assertThat(context.getBean(MeterRegistry.class).find("r2dbc.pool.acquired").gauge()).isNotNull();
			});
	}

	@Test
	void failedAllocationsAreRecorded() {
		this.contextRunner.withPropertyValues("spring.r2dbc.pool.acquire-retry=0")
			.withBean(ConnectionFactoryDecorator.class, () -> (factory) -> {
				ConnectionFactory failing = mock(ConnectionFactory.class);
				given(failing.create())
					.willAnswer((invocation) -> Mono.error(new IllegalStateException("Connection unavailable")));
				given(failing.getMetadata()).willReturn(factory.getMetadata());
				return failing;
			})
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				assertThatIllegalStateException()
					.isThrownBy(() -> Mono.from(context.getBean(ConnectionFactory.class).create()).block())
					.withMessage("Connection unavailable");
				assertThat(context.getBean(MeterRegistry.class)
					.get("reactor.pool.allocation")
					.tags("pool.name", "connectionFactory", "pool.allocation.outcome", "failure")
					.timer()
					.count()).isPositive();
			});
	}

	@Test
	void pendingAcquisitionIsRecorded() {
		this.contextRunner.withPropertyValues("spring.r2dbc.pool.initial-size=1", "spring.r2dbc.pool.max-size=1")
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				ConnectionFactory factory = context.getBean(ConnectionFactory.class);
				Connection first = Mono.from(factory.create()).block(Duration.ofSeconds(5));
				assertThat(first).isNotNull();
				CompletableFuture<? extends Connection> pending = Mono.from(factory.create()).toFuture();
				assertThat(pending).isNotDone();
				Mono.from(first.close()).block(Duration.ofSeconds(5));
				Connection second = Mono.fromFuture(pending).block(Duration.ofSeconds(5));
				assertThat(second).isNotNull();
				Mono.from(second.close()).block(Duration.ofSeconds(5));
				assertThat(context.getBean(MeterRegistry.class)
					.get("reactor.pool.pending")
					.tags("pool.name", "connectionFactory", "pool.pending.outcome", "success")
					.timer()
					.count()).isEqualTo(1);
			});
	}

	private void useConnection(ConnectionFactory connectionFactory) {
		Connection connection = Mono.from(connectionFactory.create()).block();
		assertThat(connection).isNotNull();
		Mono.from(connection.close()).block();
	}

	@Configuration(proxyBeanMethods = false)
	static class RegistryCustomizerConfiguration {

		@Bean
		MeterRegistryCustomizer<MeterRegistry> registryCustomizer(ConnectionFactory connectionFactory) {
			return (registry) -> registry.config()
				.commonTags("database", connectionFactory.getMetadata().getName().toLowerCase(Locale.ROOT));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		SimpleMeterRegistry registry() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionFactoryConfiguration {

		@Bean
		ConnectionFactory testConnectionPool() {
			return new ConnectionPool(
					ConnectionPoolConfiguration.builder(H2ConnectionFactory.inMemory("db-" + UUID.randomUUID(), "sa",
							"", Collections.singletonMap(H2ConnectionOption.DB_CLOSE_DELAY, "-1")))
						.build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WrappedConnectionPoolConfiguration {

		@Bean
		ConnectionFactory wrappedConnectionPool() {
			return new Wrapper(new ConnectionPool(
					ConnectionPoolConfiguration.builder(H2ConnectionFactory.inMemory("db-" + UUID.randomUUID(), "sa",
							"", Collections.singletonMap(H2ConnectionOption.DB_CLOSE_DELAY, "-1")))
						.build()));
		}

		static class Wrapper implements ConnectionFactory, Wrapped<ConnectionFactory> {

			private final ConnectionFactory delegate;

			Wrapper(ConnectionFactory delegate) {
				this.delegate = delegate;
			}

			@Override
			public ConnectionFactory unwrap() {
				return this.delegate;
			}

			@Override
			public Publisher<? extends Connection> create() {
				return this.delegate.create();
			}

			@Override
			public ConnectionFactoryMetadata getMetadata() {
				return this.delegate.getMetadata();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleConnectionPoolsConfiguration {

		@Bean
		CloseableConnectionFactory connectionFactory() {
			return H2ConnectionFactory.inMemory("db-" + UUID.randomUUID(), "sa", "",
					Collections.singletonMap(H2ConnectionOption.DB_CLOSE_DELAY, "-1"));
		}

		@Bean
		ConnectionPool standardPool(ConnectionFactory connectionFactory) {
			return new ConnectionPool(ConnectionPoolConfiguration.builder(connectionFactory).build());
		}

		@Bean(defaultCandidate = false)
		ConnectionPool nonDefaultPool(ConnectionFactory connectionFactory) {
			return new ConnectionPool(ConnectionPoolConfiguration.builder(connectionFactory).build());
		}

		@Bean(autowireCandidate = false)
		ConnectionPool nonAutowirePool(ConnectionFactory connectionFactory) {
			return new ConnectionPool(ConnectionPoolConfiguration.builder(connectionFactory).build());
		}

	}

}
