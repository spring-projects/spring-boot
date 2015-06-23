/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.metrics.buffer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.reader.PrefixMetricReader;
import org.springframework.lang.UsesJava8;

/**
 * {@link MetricReader} implementation using {@link CounterBuffers} and
 * {@link GaugeBuffers}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@UsesJava8
public class BufferMetricReader implements MetricReader, PrefixMetricReader {

	private final CounterBuffers counters;

	private final GaugeBuffers gauges;

	private final Predicate<String> all = Pattern.compile(".*").asPredicate();

	public BufferMetricReader(CounterBuffers counters, GaugeBuffers gauges) {
		this.counters = counters;
		this.gauges = gauges;
	}

	@Override
	public Metric<?> findOne(final String name) {
		LongBuffer buffer = this.counters.find(name);
		if (buffer != null) {
			return new Metric<Long>(name, buffer.getValue(), new Date(
					buffer.getTimestamp()));
		}
		DoubleBuffer doubleValue = this.gauges.find(name);
		if (doubleValue != null) {
			return new Metric<Double>(name, doubleValue.getValue(), new Date(
					doubleValue.getTimestamp()));
		}
		return null;
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		return findAll(this.all);
	}

	@Override
	public Iterable<Metric<?>> findAll(String prefix) {
		return findAll(Pattern.compile(prefix + ".*").asPredicate());
	}

	@Override
	public long count() {
		return this.counters.count() + this.gauges.count();
	}

	private Iterable<Metric<?>> findAll(Predicate<String> predicate) {
		final List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		this.counters.forEach(predicate, new BiConsumer<String, LongBuffer>() {

			@Override
			public void accept(String name, LongBuffer value) {
				metrics.add(new Metric<Long>(name, value.getValue(), new Date(value
						.getTimestamp())));
			}

		});
		this.gauges.forEach(predicate, new BiConsumer<String, DoubleBuffer>() {

			@Override
			public void accept(String name, DoubleBuffer value) {
				metrics.add(new Metric<Double>(name, value.getValue(), new Date(value
						.getTimestamp())));
			}

		});
		return metrics;
	}

}
