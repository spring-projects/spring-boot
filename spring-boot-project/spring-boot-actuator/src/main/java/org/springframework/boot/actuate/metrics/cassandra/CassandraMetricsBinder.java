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

package org.springframework.boot.actuate.metrics.cassandra;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.CqlSession;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.util.Assert;

/**
 * This is a {@link MeterBinder} that binds all available Cassandra DataStax driver
 * metrics to Micrometer.
 *
 * @author Erik Merkle
 * @since 2.4.0
 */
public class CassandraMetricsBinder implements MeterBinder {

	/**
	 * Prefix for all Driver configuration parameters.
	 */
	public static final String SESSION_PREFIX = "spring.data.cassandra.driver.session";

	/**
	 * Description for micrometer metrics.
	 */
	private static final String DESCRIPTION = "Please see reference.conf for more information.";

	private static final Map<String, Function<Snapshot, Number>> TIMER_SNAPSHOT_METRICS;

	private static final Map<String, Function<Timer, Number>> TIMER_METRICS;

	static {
		Map<String, Function<Snapshot, Number>> map = new HashMap<>(10);
		map.put("p999", Snapshot::get999thPercentile);
		map.put("p99", Snapshot::get99thPercentile);
		map.put("p98", Snapshot::get98thPercentile);
		map.put("p95", Snapshot::get95thPercentile);
		map.put("p75", Snapshot::get75thPercentile);
		map.put("min", Snapshot::getMin);
		map.put("max", Snapshot::getMax);
		map.put("mean", Snapshot::getMean);
		map.put("median", Snapshot::getMedian);
		map.put("stdDev", Snapshot::getStdDev);
		TIMER_SNAPSHOT_METRICS = Collections.unmodifiableMap(map);
	}

	static {
		Map<String, Function<Timer, Number>> map = new HashMap<>(4);
		map.put("mean_rate", Timer::getMeanRate);
		map.put("m1_rate", Timer::getOneMinuteRate);
		map.put("m5_rate", Timer::getFiveMinuteRate);
		map.put("m15_rate", Timer::getFifteenMinuteRate);
		TIMER_METRICS = Collections.unmodifiableMap(map);
	}

	private final Iterable<Tag> tags;

	private final CqlSession session;

	public CassandraMetricsBinder(@NonNull CqlSession cqlSession) {
		this.session = cqlSession;
		this.tags = Tags.of("sessionName", cqlSession.getName());
	}

	@Override
	public void bindTo(@NonNull MeterRegistry meterRegistry) {
		Assert.isTrue(this.session.getMetrics().isPresent(), "Metrics on the CqlSession needs to be present");

		this.session.getMetrics().get().getRegistry().getMetrics().forEach((name, m) -> {
			if (m instanceof Timer) {
				registerTimer(name, (Timer) m, meterRegistry);
			}
			else if (m instanceof Counting) {
				registerMeter(name, (Counting) m, meterRegistry);
			}
			else if (m instanceof com.codahale.metrics.Gauge) {
				registerGauge(name, (com.codahale.metrics.Gauge<?>) m, meterRegistry);
			}
		});
	}

	private void registerTimer(String metricName, Timer timer, MeterRegistry meterRegistry) {
		String name = String.format("%s.%s", SESSION_PREFIX, metricName);

		// count metric should be registered at the root level of the metric name
		FunctionCounter.builder(name, timer, Timer::getCount).tags(this.tags).description(DESCRIPTION)
				.register(meterRegistry);

		TIMER_METRICS.forEach((n, supplier) -> {
			String nameTimerMetric = String.format("%s.%s", name, n);
			FunctionCounter.builder(nameTimerMetric, timer, (t) -> supplier.apply(timer).doubleValue()).tags(this.tags)
					// all rates are calculated per second
					.baseUnit(TimeUnit.SECONDS.name()).description(DESCRIPTION).register(meterRegistry);
		});

		final Snapshot snapshot = timer.getSnapshot();
		TIMER_SNAPSHOT_METRICS.forEach((n, supplier) -> {
			String nameTimerMetric = String.format("%s.%s", name, n);
			FunctionCounter.builder(nameTimerMetric, timer, (t) -> supplier.apply(snapshot).longValue()).tags(this.tags)
					.baseUnit(TimeUnit.NANOSECONDS.name()).description(DESCRIPTION).register(meterRegistry);
		});
	}

	private void registerMeter(String metricName, Counting counter, MeterRegistry meterRegistry) {
		String name = String.format("%s.%s", SESSION_PREFIX, metricName);
		FunctionCounter.builder(name, counter, Counting::getCount).tags(this.tags).description(DESCRIPTION)
				.register(meterRegistry);
	}

	private void registerGauge(String metricName, com.codahale.metrics.Gauge<?> gauge, MeterRegistry meterRegistry) {
		String name = String.format("%s.%s", SESSION_PREFIX, metricName);
		Gauge.builder(name, gauge, (m) -> ((Number) m.getValue()).doubleValue()).tags(this.tags)
				.description(DESCRIPTION).register(meterRegistry);
	}

}
