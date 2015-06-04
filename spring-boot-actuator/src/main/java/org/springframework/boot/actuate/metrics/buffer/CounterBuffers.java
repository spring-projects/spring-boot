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

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.lang.UsesJava8;

/**
 * Fast writes to in-memory metrics store using {@link LongBuffer}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@UsesJava8
public class CounterBuffers {

	private final ConcurrentHashMap<String, LongBuffer> metrics = new ConcurrentHashMap<String, LongBuffer>();

	public void forEach(final Predicate<String> predicate,
			final BiConsumer<String, LongBuffer> consumer) {
		this.metrics.forEach(new BiConsumer<String, LongBuffer>() {
			@Override
			public void accept(String name, LongBuffer value) {
				if (predicate.test(name)) {
					consumer.accept(name, value);
				}
			}
		});
	}

	public LongBuffer find(final String name) {
		return this.metrics.get(name);
	}

	public void get(final String name, final Consumer<LongBuffer> consumer) {
		read(name, new Consumer<LongBuffer>() {
			@Override
			public void accept(LongBuffer adder) {
				consumer.accept(adder);
			}
		});
	}

	public void increment(final String name, final long delta) {
		write(name, new Consumer<LongBuffer>() {
			@Override
			public void accept(LongBuffer adder) {
				adder.add(delta);
			}
		});
	}

	public void reset(final String name) {
		write(name, new Consumer<LongBuffer>() {
			@Override
			public void accept(LongBuffer adder) {
				adder.reset();
			}
		});
	}

	public int count() {
		return this.metrics.size();
	}

	private void read(final String name, final Consumer<LongBuffer> consumer) {
		acceptInternal(name, new Consumer<LongBuffer>() {
			@Override
			public void accept(LongBuffer adder) {
				consumer.accept(adder);
			}
		});
	}

	private void write(final String name, final Consumer<LongBuffer> consumer) {
		acceptInternal(name, new Consumer<LongBuffer>() {
			@Override
			public void accept(LongBuffer buffer) {
				buffer.setTimestamp(System.currentTimeMillis());
				consumer.accept(buffer);
			}
		});
	}

	private void acceptInternal(final String name, final Consumer<LongBuffer> consumer) {
		LongBuffer adder;
		if (null == (adder = this.metrics.get(name))) {
			adder = this.metrics.computeIfAbsent(name,
					new Function<String, LongBuffer>() {
						@Override
						public LongBuffer apply(String name) {
							return new LongBuffer(0L);
						}
					});
		}
		consumer.accept(adder);
	}

}
