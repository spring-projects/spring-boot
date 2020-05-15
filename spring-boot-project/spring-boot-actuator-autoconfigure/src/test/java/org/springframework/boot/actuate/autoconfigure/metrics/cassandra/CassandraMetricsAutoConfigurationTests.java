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

package org.springframework.boot.actuate.autoconfigure.metrics.cassandra;

import java.time.Duration;
import java.util.Optional;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metrics.Metrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.cassandra.CassandraMetricsBinder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraMetricsAutoConfiguration}.
 *
 * @author Erik Merkle
 */
public class CassandraMetricsAutoConfigurationTests {

	private static final String CQL_SESSION_NAME = "mock-session";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(CassandraMetricsAutoConfiguration.class));

	/**
	 * Cassandra driver metrics should be enabled by default as long as the desired
	 * metrics are enabled in the Driver's configuration.
	 */
	@Test
	void autoConfiguredCassandraIsInstrumented() {
		this.contextRunner.withBean(CqlSession.class, () -> mockSession(true)).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			// Assert a Driver Meter metric
			assertMeterMetric(registry, "bytes-sent", 42);
			// Assert a Driver Gauge metric
			assertGaugeMetric(registry, "connected-nodes", 7);
			// Assert a Driver Timer metric
			assertTimerMetric(registry, "cql-requests", 4);
			// Assert Driver Timer snapshot metrics (min, max, avg)
			assertTimerSnapshotMetrics(registry, "cql-requests", Duration.ofMillis(10).toNanos(),
					Duration.ofMillis(40).toNanos(), Duration.ofMillis(25).toNanos());
		});
	}

	/**
	 * Cassandra driver metrics should be disabled if set to false via Spring metrics
	 * management.
	 */
	@Test
	void cassandraInstrumentationCanBeDisabledThroughSpringMetricsManagement() {
		this.contextRunner.withBean(CqlSession.class, () -> mockSession(true))
				.withPropertyValues("management.metrics.enable.spring.data.cassandra.driver.session=false")
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find(getMetricName("bytes-sent")).meter()).isNull();
					assertThat(registry.find(getMetricName("connected-nodes")).meter()).isNull();
					assertThat(registry.find(getMetricName("cql-requests")).meter()).isNull();
				});
	}

	/**
	 * Cassandra driver metrics should be disabled if no metrics are enabled in the
	 * Driver's configuration.
	 */
	@Test
	void cassandraInstrumentationIsDisabledIfNoMetricsEnabledInDriverConfig() {
		this.contextRunner.withBean(CqlSession.class, () -> mockSession(false)).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find(getMetricName("bytes-sent")).meter()).isNull();
			assertThat(registry.find(getMetricName("connected-nodes")).meter()).isNull();
			assertThat(registry.find(getMetricName("cql-requests")).meter()).isNull();
		});
	}

	/**
	 * Cassandra driver metrics should be disabled if {@link CqlSession} and
	 * {@link MetricRegistry} are not available on the classpath.
	 */
	@Test
	void cassandraInstrumentationNotAvailableIfClassesNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(CqlSession.class, MetricRegistry.class))
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find(getMetricName(".bytes-sent")).meter()).isNull();
					assertThat(registry.find(getMetricName("connected-nodes")).meter()).isNull();
					assertThat(registry.find(getMetricName("cql-requests")).meter()).isNull();
				});
	}

	private String getMetricName(String metric) {
		return CassandraMetricsBinder.SESSION_PREFIX + "." + CQL_SESSION_NAME + "." + metric;
	}

	private void assertGaugeMetric(MeterRegistry registry, String metric, double expectedValue) {
		final String metricName = getMetricName(metric);
		assertThat(registry.find(metricName).meter()).isNotNull();
		assertThat(registry.get(metricName).gauge().value()).isEqualTo(expectedValue);
	}

	private void assertMeterMetric(MeterRegistry registry, String metric, double expectedValue) {
		final String metricName = getMetricName(metric);
		assertThat(registry.find(metricName).meter()).isNotNull();
		assertThat(registry.get(metricName).functionCounter().count()).isEqualTo(expectedValue);
	}

	private void assertTimerMetric(MeterRegistry registry, String metric, double expectedValue) {
		final String metricName = getMetricName(metric);
		assertThat(registry.find(metricName).meter()).isNotNull();
		assertThat(registry.get(metricName).functionCounter().count()).isEqualTo(expectedValue);
	}

	private void assertTimerSnapshotMetrics(MeterRegistry registry, String metric, long expectedMinValue,
			long expectedMaxValue, long expectedMeanValue) {
		final String minMetricName = getMetricName(metric + ".min");
		final String maxMetricName = getMetricName(metric + ".max");
		final String meanMetricName = getMetricName(metric + ".mean");

		assertThat(registry.find(minMetricName).meter()).isNotNull();
		assertThat(registry.find(maxMetricName).meter()).isNotNull();
		assertThat(registry.find(meanMetricName).meter()).isNotNull();

		assertThat(registry.get(minMetricName).functionCounter().count()).isEqualTo(expectedMinValue);
		assertThat(registry.get(maxMetricName).functionCounter().count()).isEqualTo(expectedMaxValue);
		assertThat(registry.get(meanMetricName).functionCounter().count()).isEqualTo(expectedMeanValue);
	}

	private CqlSession mockSession(boolean metricsEnabled) {
		CqlSession session = mock(CqlSession.class);
		given(session.getName()).willReturn(CQL_SESSION_NAME);
		if (metricsEnabled) {
			MetricRegistry registry = new MetricRegistry();
			// register a Driver Counter
			registry.meter(CQL_SESSION_NAME + ".bytes-sent").mark(42);
			// register a Driver Gauge
			registry.gauge(CQL_SESSION_NAME + ".connected-nodes", () -> new Gauge() {
				@Override
				public Object getValue() {
					return 7;
				}
			});
			// register a Driver Timer
			final Timer cqlRequestTimer = new Timer();
			// simulate 4 timer pegs with durations of 10ms, 20ms, 30ms and 40ms
			cqlRequestTimer.update(Duration.ofMillis(10));
			cqlRequestTimer.update(Duration.ofMillis(20));
			cqlRequestTimer.update(Duration.ofMillis(30));
			cqlRequestTimer.update(Duration.ofMillis(40));
			registry.timer(CQL_SESSION_NAME + ".cql-requests", new MetricRegistry.MetricSupplier<Timer>() {
				@Override
				public Timer newMetric() {
					return cqlRequestTimer;
				}
			});
			Metrics metrics = mock(Metrics.class);
			given(metrics.getRegistry()).willReturn(registry);
			given(session.getMetrics()).willReturn(Optional.of(metrics));
		}
		else {
			given(session.getMetrics()).willReturn(Optional.empty());
		}
		return session;
	}

}
