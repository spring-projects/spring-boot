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

import java.util.concurrent.atomic.LongAdder;

import org.springframework.lang.UsesJava8;

/**
 * Mutable buffer containing a long adder (Java 8) and a timestamp.
 *
 * @author Dave Syer
 */
@UsesJava8
public class LongBuffer {

	private final LongAdder adder;

	private volatile long timestamp;

	public LongBuffer(long timestamp) {
		this.adder = new LongAdder();
		this.timestamp = timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getValue() {
		return this.adder.sum();
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public void reset() {
		this.adder.reset();
	}

	public void add(long delta) {
		this.adder.add(delta);
	}
}