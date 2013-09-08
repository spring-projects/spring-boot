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

package org.springframework.boot.actuate.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link InMemoryMetricRepository}.
 */
public class InMemoryMetricRepositoryTests {

	private InMemoryMetricRepository repository = new InMemoryMetricRepository();

	@Test
	public void increment() {
		this.repository.increment("foo", 1, new Date());
		assertEquals(1.0, this.repository.findOne("foo").getValue(), 0.01);
	}

	@Test
	public void incrementConcurrent() throws Exception {
		Collection<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
		for (int i = 0; i < 100; i++) {
			tasks.add(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					InMemoryMetricRepositoryTests.this.repository.increment("foo", 1,
							new Date());
					return true;
				}
			});
			tasks.add(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					InMemoryMetricRepositoryTests.this.repository.increment("foo", -1,
							new Date());
					return true;
				}
			});
		}
		List<Future<Boolean>> all = Executors.newFixedThreadPool(10).invokeAll(tasks);
		for (Future<Boolean> future : all) {
			assertTrue(future.get(1, TimeUnit.SECONDS));
		}
		assertEquals(0, this.repository.findOne("foo").getValue(), 0.01);
	}

	@Test
	public void set() {
		this.repository.set("foo", 1, new Date());
		assertEquals(1.0, this.repository.findOne("foo").getValue(), 0.01);
	}

}
