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

import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link CounterBuffers}.
 *
 * @author Dave Syer
 */
public class CounterBuffersTests {

	private CounterBuffers buffers = new CounterBuffers();

	private long value;

	@Test
	public void inAndOut() {
		this.buffers.increment("foo", 2);
		this.buffers.get("foo", new Consumer<LongBuffer>() {
			@Override
			public void accept(LongBuffer buffer) {
				CounterBuffersTests.this.value = buffer.getValue();
			}
		});
		assertEquals(2, this.value);
	}

	@Test
	public void getNonExistent() {
		this.buffers.get("foo", new Consumer<LongBuffer>() {
			@Override
			public void accept(LongBuffer buffer) {
				CounterBuffersTests.this.value = buffer.getValue();
			}
		});
		assertEquals(0, this.value);
	}

	@Test
	public void findNonExistent() {
		assertNull(this.buffers.find("foo"));
	}
}
