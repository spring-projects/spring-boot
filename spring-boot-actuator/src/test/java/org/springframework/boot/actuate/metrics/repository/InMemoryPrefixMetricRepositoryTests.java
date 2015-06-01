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

package org.springframework.boot.actuate.metrics.repository;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class InMemoryPrefixMetricRepositoryTests {

	private final InMemoryMetricRepository repository = new InMemoryMetricRepository();

	@Test
	public void registeredPrefixCounted() {
		this.repository.increment(new Delta<Number>("foo.bar", 1));
		this.repository.increment(new Delta<Number>("foo.bar", 1));
		this.repository.increment(new Delta<Number>("foo.spam", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo")) {
			names.add(metric.getName());
		}
		assertEquals(2, names.size());
		assertTrue(names.contains("foo.bar"));
	}

	@Test
	public void prefixWithWildcard() {
		this.repository.increment(new Delta<Number>("foo.bar", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo.*")) {
			names.add(metric.getName());
		}
		assertEquals(1, names.size());
		assertTrue(names.contains("foo.bar"));
	}

	@Test
	public void prefixWithPeriod() {
		this.repository.increment(new Delta<Number>("foo.bar", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo.")) {
			names.add(metric.getName());
		}
		assertEquals(1, names.size());
		assertTrue(names.contains("foo.bar"));
	}

	@Test
	public void onlyRegisteredPrefixCounted() {
		this.repository.increment(new Delta<Number>("foo.bar", 1));
		this.repository.increment(new Delta<Number>("foobar.spam", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo")) {
			names.add(metric.getName());
		}
		assertEquals(1, names.size());
		assertTrue(names.contains("foo.bar"));
	}

	@Test
	public void incrementGroup() {
		this.repository.increment("foo", new Delta<Number>("foo.bar", 1));
		this.repository.increment("foo", new Delta<Number>("foo.bar", 2));
		this.repository.increment("foo", new Delta<Number>("foo.spam", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo")) {
			names.add(metric.getName());
		}
		assertEquals(2, names.size());
		assertTrue(names.contains("foo.bar"));
		assertEquals(3L, this.repository.findOne("foo.bar").getValue());
	}

}
