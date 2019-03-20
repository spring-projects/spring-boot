/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.metrics.buffer;

import java.util.function.Consumer;

import org.springframework.lang.UsesJava8;

/**
 * Fast writes to in-memory metrics store using {@link CounterBuffer}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@UsesJava8
public class CounterBuffers extends Buffers<CounterBuffer> {

	public void increment(final String name, final long delta) {
		doWith(name, new Consumer<CounterBuffer>() {

			@Override
			public void accept(CounterBuffer buffer) {
				buffer.setTimestamp(System.currentTimeMillis());
				buffer.add(delta);
			}

		});
	}

	public void reset(final String name) {
		doWith(name, new Consumer<CounterBuffer>() {

			@Override
			public void accept(CounterBuffer buffer) {
				buffer.setTimestamp(System.currentTimeMillis());
				buffer.reset();
			}

		});
	}

	@Override
	protected CounterBuffer createBuffer() {
		return new CounterBuffer(0);
	}

}
