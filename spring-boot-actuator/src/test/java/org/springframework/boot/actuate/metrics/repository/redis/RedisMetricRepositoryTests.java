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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.Iterables;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Dave Syer
 */
public class RedisMetricRepositoryTests {

	@Rule
	public RedisServer redis = RedisServer.running();
	private RedisMetricRepository repository;
	private String prefix;

	@Before
	public void init() {
		this.repository = new RedisMetricRepository(this.redis.getResource());
		this.prefix = "spring.test." + System.currentTimeMillis();
		this.repository.setPrefix(this.prefix);
	}

	@After
	public void clear() {
		assertNotNull(new StringRedisTemplate(this.redis.getResource()).opsForValue()
				.get(this.prefix + ".foo"));
		this.repository.reset("foo");
		this.repository.reset("bar");
		assertNull(new StringRedisTemplate(this.redis.getResource()).opsForValue().get(
				this.prefix + ".foo"));
	}

	@Test
	public void setAndGet() {
		this.repository.set(new Metric<Number>("foo", 12.3));
		Metric<?> metric = this.repository.findOne("foo");
		assertEquals("foo", metric.getName());
		assertEquals(12.3, metric.getValue().doubleValue(), 0.01);
	}

	@Test
	public void incrementAndGet() {
		this.repository.increment(new Delta<Long>("foo", 3L));
		assertEquals(3, this.repository.findOne("foo").getValue().longValue());
	}

	@Test
	public void setIncrementAndGet() {
		this.repository.set(new Metric<Number>("foo", 12.3));
		this.repository.increment(new Delta<Long>("foo", 3L));
		Metric<?> metric = this.repository.findOne("foo");
		assertEquals("foo", metric.getName());
		assertEquals(15.3, metric.getValue().doubleValue(), 0.01);
	}

	@Test
	public void findAll() {
		this.repository.increment(new Delta<Long>("foo", 3L));
		this.repository.set(new Metric<Number>("bar", 12.3));
		assertEquals(2, Iterables.collection(this.repository.findAll()).size());
	}

	@Test
	public void findOneWithAll() {
		this.repository.increment(new Delta<Long>("foo", 3L));
		Metric<?> metric = this.repository.findAll().iterator().next();
		assertEquals("foo", metric.getName());
	}

	@Test
	public void count() {
		this.repository.increment(new Delta<Long>("foo", 3L));
		this.repository.set(new Metric<Number>("bar", 12.3));
		assertEquals(2, this.repository.count());
	}

}
