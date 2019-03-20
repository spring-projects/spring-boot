/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.metrics.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.util.Assert;

/**
 * {@link MultiMetricRepository} implementation backed by a
 * {@link InMemoryMetricRepository}.
 *
 * @author Stephane Nicoll
 * @since 1.5.0
 */
public class InMemoryMultiMetricRepository implements MultiMetricRepository {

	private final InMemoryMetricRepository repository;

	private final Collection<String> groups = new HashSet<String>();

	/**
	 * Create a new {@link InMemoryMetricRepository} backed by a new
	 * {@link InMemoryMetricRepository}.
	 */
	public InMemoryMultiMetricRepository() {
		this(new InMemoryMetricRepository());
	}

	/**
	 * Create a new {@link InMemoryMetricRepository} backed by the specified
	 * {@link InMemoryMetricRepository}.
	 * @param repository the backing repository
	 */
	public InMemoryMultiMetricRepository(InMemoryMetricRepository repository) {
		Assert.notNull(repository, "Repository must not be null");
		this.repository = repository;
	}

	@Override
	public void set(String group, Collection<Metric<?>> values) {
		String prefix = group;
		if (!prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		for (Metric<?> metric : values) {
			if (!metric.getName().startsWith(prefix)) {
				metric = new Metric<Number>(prefix + metric.getName(), metric.getValue(),
						metric.getTimestamp());
			}
			this.repository.set(metric);
		}
		this.groups.add(group);
	}

	@Override
	public void increment(String group, Delta<?> delta) {
		String prefix = group;
		if (!prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		if (!delta.getName().startsWith(prefix)) {
			delta = new Delta<Number>(prefix + delta.getName(), delta.getValue(),
					delta.getTimestamp());
		}
		this.repository.increment(delta);
		this.groups.add(group);
	}

	@Override
	public Iterable<String> groups() {
		return Collections.unmodifiableCollection(this.groups);
	}

	@Override
	public long countGroups() {
		return this.groups.size();
	}

	@Override
	public void reset(String group) {
		for (Metric<?> metric : findAll(group)) {
			this.repository.reset(metric.getName());
		}
		this.groups.remove(group);
	}

	@Override
	public Iterable<Metric<?>> findAll(String metricNamePrefix) {
		return this.repository.findAllWithPrefix(metricNamePrefix);
	}

}
