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
 * @since 1.3.0
 */
@UsesJava8
public class CounterBuffer extends Buffer<Long> {

	private final LongAdder adder;

	public CounterBuffer(long timestamp) {
		super(timestamp);
		this.adder = new LongAdder();
	}

	public void add(long delta) {
		this.adder.add(delta);
	}

	public void reset() {
		this.adder.reset();
	}

	@Override
	public Long getValue() {
		return this.adder.sum();
	}

}
