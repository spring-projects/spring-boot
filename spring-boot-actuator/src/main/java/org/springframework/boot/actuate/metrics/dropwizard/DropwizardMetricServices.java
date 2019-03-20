/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.metrics.dropwizard;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Timer;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * A {@link GaugeService} and {@link CounterService} that sends data to a Dropwizard
 * {@link MetricRegistry} based on a naming convention.
 * <ul>
 * <li>Updates to {@link #increment(String)} with names in "meter.*" are treated as
 * {@link Meter} events</li>
 * <li>Other deltas are treated as simple {@link Counter} values</li>
 * <li>Inputs to {@link #submit(String, double)} with names in "histogram.*" are treated
 * as {@link Histogram} updates</li>
 * <li>Inputs to {@link #submit(String, double)} with names in "timer.*" are treated as
 * {@link Timer} updates</li>
 * <li>Other metrics are treated as simple {@link Gauge} values (single valued
 * measurements of type double)</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Jay Anderson
 * @author Andy Wilkinson
 */
public class DropwizardMetricServices implements CounterService, GaugeService {

	private final MetricRegistry registry;

	private final ReservoirFactory reservoirFactory;

	private final ConcurrentMap<String, SimpleGauge> gauges = new ConcurrentHashMap<String, SimpleGauge>();

	private final ConcurrentHashMap<String, String> names = new ConcurrentHashMap<String, String>();

	/**
	 * Create a new {@link DropwizardMetricServices} instance.
	 * @param registry the underlying metric registry
	 */
	public DropwizardMetricServices(MetricRegistry registry) {
		this(registry, null);
	}

	/**
	 * Create a new {@link DropwizardMetricServices} instance.
	 * @param registry the underlying metric registry
	 * @param reservoirFactory the factory that instantiates the {@link Reservoir} that
	 * will be used on Timers and Histograms
	 */
	public DropwizardMetricServices(MetricRegistry registry,
			ReservoirFactory reservoirFactory) {
		this.registry = registry;
		this.reservoirFactory = (reservoirFactory != null) ? reservoirFactory
				: ReservoirFactory.NONE;
	}

	@Override
	public void increment(String name) {
		incrementInternal(name, 1L);
	}

	@Override
	public void decrement(String name) {
		incrementInternal(name, -1L);
	}

	private void incrementInternal(String name, long value) {
		if (name.startsWith("meter")) {
			Meter meter = this.registry.meter(name);
			meter.mark(value);
		}
		else {
			name = wrapCounterName(name);
			Counter counter = this.registry.counter(name);
			counter.inc(value);
		}
	}

	@Override
	public void submit(String name, double value) {
		if (name.startsWith("histogram")) {
			submitHistogram(name, value);
		}
		else if (name.startsWith("timer")) {
			submitTimer(name, value);
		}
		else {
			name = wrapGaugeName(name);
			setGaugeValue(name, value);
		}
	}

	private void submitTimer(String name, double value) {
		long longValue = (long) value;
		Timer metric = register(name, new TimerMetricRegistrar());
		metric.update(longValue, TimeUnit.MILLISECONDS);
	}

	private void submitHistogram(String name, double value) {
		long longValue = (long) value;
		Histogram metric = register(name, new HistogramMetricRegistrar());
		metric.update(longValue);
	}

	@SuppressWarnings("unchecked")
	private <T extends Metric> T register(String name, MetricRegistrar<T> registrar) {
		Reservoir reservoir = this.reservoirFactory.getReservoir(name);
		if (reservoir == null) {
			return registrar.register(this.registry, name);
		}
		Metric metric = this.registry.getMetrics().get(name);
		if (metric != null) {
			registrar.checkExisting(metric);
			return (T) metric;
		}
		try {
			return this.registry.register(name, registrar.createForReservoir(reservoir));
		}
		catch (IllegalArgumentException ex) {
			Metric added = this.registry.getMetrics().get(name);
			registrar.checkExisting(added);
			return (T) added;
		}
	}

	private void setGaugeValue(String name, double value) {
		// NOTE: Dropwizard provides no way to do this atomically
		SimpleGauge gauge = this.gauges.get(name);
		if (gauge == null) {
			SimpleGauge newGauge = new SimpleGauge(value);
			gauge = this.gauges.putIfAbsent(name, newGauge);
			if (gauge == null) {
				this.registry.register(name, newGauge);
				return;
			}
		}
		gauge.setValue(value);
	}

	private String wrapGaugeName(String metricName) {
		return wrapName(metricName, "gauge.");
	}

	private String wrapCounterName(String metricName) {
		return wrapName(metricName, "counter.");
	}

	private String wrapName(String metricName, String prefix) {
		String cached = this.names.get(metricName);
		if (cached != null) {
			return cached;
		}
		if (metricName.startsWith(prefix)) {
			return metricName;
		}
		String name = prefix + metricName;
		this.names.put(metricName, name);
		return name;
	}

	@Override
	public void reset(String name) {
		if (!name.startsWith("meter")) {
			name = wrapCounterName(name);
		}
		this.registry.remove(name);
	}

	/**
	 * Simple {@link Gauge} implementation to {@literal double} value.
	 */
	private static final class SimpleGauge implements Gauge<Double> {

		private volatile double value;

		private SimpleGauge(double value) {
			this.value = value;
		}

		@Override
		public Double getValue() {
			return this.value;
		}

		public void setValue(double value) {
			this.value = value;
		}

	}

	/**
	 * Strategy used to register metrics.
	 */
	private abstract static class MetricRegistrar<T extends Metric> {

		private final Class<T> type;

		@SuppressWarnings("unchecked")
		MetricRegistrar() {
			this.type = (Class<T>) ResolvableType
					.forClass(MetricRegistrar.class, getClass()).resolveGeneric();
		}

		public void checkExisting(Metric metric) {
			Assert.isInstanceOf(this.type, metric,
					"Different metric type already registered");
		}

		protected abstract T register(MetricRegistry registry, String name);

		protected abstract T createForReservoir(Reservoir reservoir);

	}

	/**
	 * {@link MetricRegistrar} for {@link Timer} metrics.
	 */
	private static class TimerMetricRegistrar extends MetricRegistrar<Timer> {

		@Override
		protected Timer register(MetricRegistry registry, String name) {
			return registry.timer(name);
		}

		@Override
		protected Timer createForReservoir(Reservoir reservoir) {
			return new Timer(reservoir);
		}

	}

	/**
	 * {@link MetricRegistrar} for {@link Histogram} metrics.
	 */
	private static class HistogramMetricRegistrar extends MetricRegistrar<Histogram> {

		@Override
		protected Histogram register(MetricRegistry registry, String name) {
			return registry.histogram(name);
		}

		@Override
		protected Histogram createForReservoir(Reservoir reservoir) {
			return new Histogram(reservoir);
		}

	}

}
