/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.repository;

import java.util.Date;
import java.util.concurrent.ConcurrentNavigableMap;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.util.SimpleInMemoryRepository;
import org.springframework.boot.actuate.metrics.writer.Delta;

/**
 * {@link MetricRepository} implementation that stores metrics in memory.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class InMemoryMetricRepository implements MetricRepository {

	private final SimpleInMemoryRepository<Metric<?>> metrics = new SimpleInMemoryRepository<>();

	public void setValues(ConcurrentNavigableMap<String, Metric<?>> values) {
		this.metrics.setValues(values);
	}

	@Override
	public void increment(Delta<?> delta) {
		final String metricName = delta.getName();
		final int amount = delta.getValue().intValue();
		final Date timestamp = delta.getTimestamp();
		this.metrics.update(metricName, (current) -> {
			if (current != null) {
				return new Metric<>(metricName, current.increment(amount).getValue(),
						timestamp);
			}
			return new Metric<>(metricName, (long) amount, timestamp);
		});
	}

	@Override
	public void set(Metric<?> value) {
		this.metrics.set(value.getName(), value);
	}

	@Override
	public long count() {
		return this.metrics.count();
	}

	@Override
	public void reset(String metricName) {
		this.metrics.remove(metricName);
	}

	@Override
	public Metric<?> findOne(String metricName) {
		return this.metrics.findOne(metricName);
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		return this.metrics.findAll();
	}

	public Iterable<Metric<?>> findAllWithPrefix(String prefix) {
		return this.metrics.findAllWithPrefix(prefix);
	}

}
