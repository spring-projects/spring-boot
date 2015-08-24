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

	private static final Predicate<String> ALL = Pattern.compile(".*").asPredicate();

	private final CounterBuffers counterBuffers;

	private final GaugeBuffers gaugeBuffers;

	public BufferMetricReader(CounterBuffers counterBuffers, GaugeBuffers gaugeBuffers) {
		this.counterBuffers = counterBuffers;
		this.gaugeBuffers = gaugeBuffers;
	}

	@Override
	public Metric<?> findOne(final String name) {
		Buffer<?> buffer = this.counterBuffers.find(name);
		if (buffer == null) {
			buffer = this.gaugeBuffers.find(name);
		}
		return (buffer == null ? null : asMetric(name, buffer));
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		return findAll(BufferMetricReader.ALL);
	}

	@Override
	public Iterable<Metric<?>> findAll(String prefix) {
		return findAll(Pattern.compile(prefix + ".*").asPredicate());
	}

	@Override
	public long count() {
		return this.counterBuffers.count() + this.gaugeBuffers.count();
	}

	private Iterable<Metric<?>> findAll(Predicate<String> predicate) {
		final List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		collectMetrics(this.gaugeBuffers, predicate, metrics);
		collectMetrics(this.counterBuffers, predicate, metrics);
		return metrics;
	}

	private <T extends Number, B extends Buffer<T>> void collectMetrics(
			Buffers<B> buffers, Predicate<String> predicate, final List<Metric<?>> metrics) {
		buffers.forEach(predicate, new BiConsumer<String, B>() {

			@Override
			public void accept(String name, B value) {
				metrics.add(asMetric(name, value));
			}

		});
	}

	private <T extends Number> Metric<T> asMetric(final String name, Buffer<T> buffer) {
		return new Metric<T>(name, buffer.getValue(), new Date(buffer.getTimestamp()));
	}

}
