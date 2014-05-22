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

package org.springframework.boot.actuate.metrics.repository.redis;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.boot.actuate.metrics.Iterables;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
@RunWith(Parameterized.class)
public class RedisMultiMetricRepositoryTests {

	@Rule
	public RedisServer redis = RedisServer.running();
	private RedisMultiMetricRepository repository;
	@Parameter(0)
	public String prefix;

	@Parameters
	public static List<Object[]> parameters() {
		return Arrays.<Object[]> asList(new Object[] { null }, new Object[] { "test" });
	}

	@Before
	public void init() {
		if (this.prefix == null) {
			this.prefix = "spring.groups";
			this.repository = new RedisMultiMetricRepository(this.redis.getResource());
		}
		else {
			this.repository = new RedisMultiMetricRepository(this.redis.getResource(),
					this.prefix);
		}
	}

	@After
	public void clear() {
		assertTrue(new StringRedisTemplate(this.redis.getResource()).opsForZSet().size(
				"keys." + this.prefix) > 0);
		this.repository.reset("foo");
		this.repository.reset("bar");
		assertNull(new StringRedisTemplate(this.redis.getResource()).opsForValue().get(
				this.prefix + ".foo"));
		assertNull(new StringRedisTemplate(this.redis.getResource()).opsForValue().get(
				this.prefix + ".bar"));
	}

	@Test
	public void setAndGet() {
		this.repository.set("foo",
				Arrays.<Metric<?>> asList(new Metric<Number>("foo.bar", 12.3)));
		this.repository.set("foo",
				Arrays.<Metric<?>> asList(new Metric<Number>("foo.bar", 15.3)));
		assertEquals(15.3, Iterables.collection(this.repository.findAll("foo"))
				.iterator().next().getValue());
	}

	@Test
	public void setAndGetMultiple() {
		this.repository.set("foo", Arrays.<Metric<?>> asList(new Metric<Number>(
				"foo.val", 12.3), new Metric<Number>("foo.bar", 11.3)));
		assertEquals(2, Iterables.collection(this.repository.findAll("foo")).size());
	}

	@Test
	public void groups() {
		this.repository.set("foo", Arrays.<Metric<?>> asList(new Metric<Number>(
				"foo.val", 12.3), new Metric<Number>("foo.bar", 11.3)));
		this.repository.set("bar", Arrays.<Metric<?>> asList(new Metric<Number>(
				"bar.val", 12.3), new Metric<Number>("bar.foo", 11.3)));
		Collection<String> groups = Iterables.collection(this.repository.groups());
		assertEquals(2, groups.size());
		assertTrue("Wrong groups: " + groups, groups.contains("foo"));
	}

	@Test
	public void count() {
		this.repository.set("foo", Arrays.<Metric<?>> asList(new Metric<Number>(
				"foo.val", 12.3), new Metric<Number>("foo.bar", 11.3)));
		this.repository.set("bar", Arrays.<Metric<?>> asList(new Metric<Number>(
				"bar.val", 12.3), new Metric<Number>("bar.foo", 11.3)));
		assertEquals(2, this.repository.countGroups());
	}

	@Test
	public void increment() {
		this.repository.increment("foo", new Delta<Number>("foo.bar", 1));
		this.repository.increment("foo", new Delta<Number>("foo.bar", 2));
		this.repository.increment("foo", new Delta<Number>("foo.spam", 1));
		Metric<?> bar = null;
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo")) {
			names.add(metric.getName());
			if (metric.getName().equals("foo.bar")) {
				bar = metric;
			}
		}
		assertEquals(2, names.size());
		assertTrue("Wrong names: " + names, names.contains("foo.bar"));
		assertEquals(3d, bar.getValue());
	}
}
