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
import java.util.List;

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
		this.repository.save("foo", Arrays.<Metric<?>> asList(new Metric<Number>(
				"foo.val", 12.3), new Metric<Number>("foo.bar", 11.3)));
		assertEquals(2, Iterables.collection(this.repository.findAll("foo")).size());
	}

	@Test
	public void groups() {
		this.repository.save("foo", Arrays.<Metric<?>> asList(new Metric<Number>(
				"foo.val", 12.3), new Metric<Number>("foo.bar", 11.3)));
		this.repository.save("bar", Arrays.<Metric<?>> asList(new Metric<Number>(
				"bar.val", 12.3), new Metric<Number>("bar.foo", 11.3)));
		assertEquals(2, Iterables.collection(this.repository.groups()).size());
	}

	@Test
	public void count() {
		this.repository.save("foo", Arrays.<Metric<?>> asList(new Metric<Number>(
				"foo.val", 12.3), new Metric<Number>("foo.bar", 11.3)));
		this.repository.save("bar", Arrays.<Metric<?>> asList(new Metric<Number>(
				"bar.val", 12.3), new Metric<Number>("bar.foo", 11.3)));
		assertEquals(2, this.repository.countGroups());
	}

}
