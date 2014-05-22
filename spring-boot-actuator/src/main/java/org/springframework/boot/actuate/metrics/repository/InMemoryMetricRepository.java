/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentNavigableMap;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.PrefixMetricReader;
import org.springframework.boot.actuate.metrics.util.SimpleInMemoryRepository;
import org.springframework.boot.actuate.metrics.util.SimpleInMemoryRepository.Callback;
import org.springframework.boot.actuate.metrics.writer.Delta;

/**
 * {@link MetricRepository} and {@link MultiMetricRepository} implementation that stores
 * metrics in memory.
 * 
 * @author Dave Syer
 */
public class InMemoryMetricRepository implements MetricRepository, MultiMetricRepository,
		PrefixMetricReader {

	private final SimpleInMemoryRepository<Metric<?>> metrics = new SimpleInMemoryRepository<Metric<?>>();

	private final Collection<String> groups = new HashSet<String>();

	public void setValues(ConcurrentNavigableMap<String, Metric<?>> values) {
		this.metrics.setValues(values);
	}

	@Override
	public void increment(Delta<?> delta) {
		final String metricName = delta.getName();
		final int amount = delta.getValue().intValue();
		final Date timestamp = delta.getTimestamp();
		this.metrics.update(metricName, new Callback<Metric<?>>() {
			@Override
			public Metric<?> modify(Metric<?> current) {
				if (current != null) {
					Metric<? extends Number> metric = current;
					return new Metric<Long>(metricName, metric.increment(amount)
							.getValue(), timestamp);
				}
				else {
					return new Metric<Long>(metricName, new Long(amount), timestamp);
				}
			}
		});
	}

	@Override
	public void set(Metric<?> value) {
		this.metrics.set(value.getName(), value);
	}

	@Override
	public void save(String group, Collection<Metric<?>> values) {
		String prefix = group;
		if (!prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		for (Metric<?> metric : values) {
			if (!metric.getName().startsWith(prefix)) {
				metric = new Metric<Number>(prefix + metric.getName(), metric.getValue(),
						metric.getTimestamp());
			}
			set(metric);
		}
		this.groups.add(group);
	}

	@Override
	public Iterable<String> groups() {
		return Collections.unmodifiableCollection(this.groups);
	}

	@Override
	public long count() {
		return this.metrics.count();
	}

	@Override
	public long countGroups() {
		return this.groups.size();
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

	@Override
	public Iterable<Metric<?>> findAll(String metricNamePrefix) {
		return this.metrics.findAllWithPrefix(metricNamePrefix);
	}

}
