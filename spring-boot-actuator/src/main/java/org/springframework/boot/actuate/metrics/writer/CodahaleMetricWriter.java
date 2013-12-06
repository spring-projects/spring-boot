/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.writer;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.metrics.Metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * A {@link MetricWriter} that send data to a Codahale {@link MetricRegistry} based on a
 * naming convention:
 * <ul>
 * <li>Updates to {@link #increment(Delta)} with names in "meter.*" are treated as
 * {@link Meter} events</li>
 * <li>Other deltas are treated as simple {@link Counter} values</li>
 * <li>Inputs to {@link #set(Metric)} with names in "histogram.*" are treated as
 * {@link Histogram} updates</li>
 * <li>Inputs to {@link #set(Metric)} with names in "timer.*" are treated as {@link Timer}
 * updates</li>
 * <li>Other metrics are treated as simple {@link Gauge} values (single valued
 * measurements of type double)</li>
 * </ul>
 * 
 * @author Dave Syer
 */
public class CodahaleMetricWriter implements MetricWriter {

	private final MetricRegistry registry;

	/**
	 * @param registry
	 */
	public CodahaleMetricWriter(MetricRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void increment(Delta<?> delta) {
		String name = delta.getName();
		long value = delta.getValue().longValue();
		if (name.startsWith("meter")) {
			Meter meter = this.registry.meter(name);
			meter.mark(value);
		}
		else {
			Counter counter = this.registry.counter(name);
			counter.inc(value);
		}
	}

	@Override
	public void set(Metric<?> value) {
		String name = value.getName();
		if (name.startsWith("histogram")) {
			long longValue = value.getValue().longValue();
			Histogram metric = this.registry.histogram(name);
			metric.update(longValue);
		}
		else if (name.startsWith("timer")) {
			long longValue = value.getValue().longValue();
			Timer metric = this.registry.timer(name);
			metric.update(longValue, TimeUnit.MILLISECONDS);
		}
		else {
			final double gauge = value.getValue().doubleValue();
			this.registry.remove(name);
			this.registry.register(name, new SimpleGauge(gauge));
		}
	}

	@Override
	public void reset(String metricName) {
		this.registry.remove(metricName);
	}

	private static class SimpleGauge implements Gauge<Double> {

		private final double gauge;

		private SimpleGauge(double gauge) {
			this.gauge = gauge;
		}

		@Override
		public Double getValue() {
			return this.gauge;
		}
	}

}
