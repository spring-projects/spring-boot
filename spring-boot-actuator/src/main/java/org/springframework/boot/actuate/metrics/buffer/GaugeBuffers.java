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
 * Fast writes to in-memory metrics store using {@link DoubleBuffer}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@UsesJava8
public class GaugeBuffers {

	private final ConcurrentHashMap<String, DoubleBuffer> metrics = new ConcurrentHashMap<String, DoubleBuffer>();

	public void forEach(final Predicate<String> predicate,
			final BiConsumer<String, DoubleBuffer> consumer) {
		this.metrics.forEach(new BiConsumer<String, DoubleBuffer>() {
			@Override
			public void accept(String name, DoubleBuffer value) {
				if (predicate.test(name)) {
					consumer.accept(name, value);
				}
			}
		});
	}

	public DoubleBuffer find(final String name) {
		return this.metrics.get(name);
	}

	public void get(final String name, final Consumer<DoubleBuffer> consumer) {
		acceptInternal(name, consumer);
	}

	public void set(final String name, final double value) {
		write(name, value);
	}

	public int count() {
		return this.metrics.size();
	}

	private void write(final String name, final double value) {
		acceptInternal(name, new Consumer<DoubleBuffer>() {
			@Override
			public void accept(DoubleBuffer buffer) {
				buffer.setTimestamp(System.currentTimeMillis());
				buffer.setValue(value);
			}
		});
	}

	public void reset(String name) {
		this.metrics.remove(name, this.metrics.get(name));
	}

	private void acceptInternal(final String name, final Consumer<DoubleBuffer> consumer) {
		DoubleBuffer value;
		if (null == (value = this.metrics.get(name))) {
			value = this.metrics.computeIfAbsent(name,
					new Function<String, DoubleBuffer>() {
						@Override
						public DoubleBuffer apply(String tag) {
							return new DoubleBuffer(0L);
						}
					});
		}
		consumer.accept(value);
	}

}
