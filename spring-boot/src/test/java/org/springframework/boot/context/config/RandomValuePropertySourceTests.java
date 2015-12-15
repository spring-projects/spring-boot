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

package org.springframework.boot.context.config;

import java.util.Random;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link RandomValuePropertySource}.
 *
 * @author Dave Syer
 * @author Matt Benson
 */
public class RandomValuePropertySourceTests {

	private RandomValuePropertySource source = new RandomValuePropertySource();

	@Test
	public void notRandom() {
		assertNull(this.source.getProperty("foo"));
	}

	@Test
	public void string() {
		assertNotNull(this.source.getProperty("random.string"));
	}

	@Test
	public void intValue() {
		Integer value = (Integer) this.source.getProperty("random.int");
		assertNotNull(value);
	}

	@Test
	public void intRange() {
		Integer value = (Integer) this.source.getProperty("random.int[4,10]");
		assertNotNull(value);
		assertTrue(value >= 4);
		assertTrue(value < 10);
	}

	@Test
	public void intMax() {
		Integer value = (Integer) this.source.getProperty("random.int(10)");
		assertNotNull(value);
		assertTrue(value < 10);
	}

	@Test
	public void longValue() {
		Long value = (Long) this.source.getProperty("random.long");
		assertNotNull(value);
	}

	@Test
	public void longRange() {
		Long value = (Long) this.source.getProperty("random.long[4,10]");
		assertNotNull(value);
		assertTrue(Long.toString(value), value >= 4L);
		assertTrue(Long.toString(value), value < 10L);
	}

	@Test
	public void longMax() {
		Long value = (Long) this.source.getProperty("random.long(10)");
		assertNotNull(value);
		assertTrue(value < 10L);
	}

	@Test
	public void longOverflow() {
		RandomValuePropertySource source = Mockito.spy(this.source);
		Mockito.when(source.getSource()).thenReturn(new Random() {

			@Override
			public long nextLong() {
				// constant that used to become -8, now becomes 8
				return Long.MIN_VALUE;
			}

		});
		Long value = (Long) source.getProperty("random.long(10)");
		assertNotNull(value);
		assertTrue(value + " is less than 0", value >= 0L);
		assertTrue(value + " is more than 10", value < 10L);

		value = (Long) source.getProperty("random.long[4,10]");
		assertNotNull(value);
		assertTrue(value + " is less than 4", value >= 4L);
		assertTrue(value + " is more than 10", value < 10L);
	}
}
