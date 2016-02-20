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
 * Base class used to manage a map of {@link Buffer} objects.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @param <B> The buffer type
 */
@UsesJava8
abstract class Buffers<B extends Buffer<?>> {

	private final ConcurrentHashMap<String, B> buffers = new ConcurrentHashMap<String, B>();

	public void forEach(final Predicate<String> predicate,
			final BiConsumer<String, B> consumer) {
		this.buffers.forEach(new BiConsumer<String, B>() {

			@Override
			public void accept(String name, B value) {
				if (predicate.test(name)) {
					consumer.accept(name, value);
				}
			}

		});
	}

	public B find(final String name) {
		return this.buffers.get(name);
	}

	public int count() {
		return this.buffers.size();
	}

	protected final void doWith(final String name, final Consumer<B> consumer) {
		B buffer = this.buffers.get(name);
		if (buffer == null) {
			buffer = this.buffers.computeIfAbsent(name, new Function<String, B>() {
				@Override
				public B apply(String name) {
					return createBuffer();
				}
			});
		}
		consumer.accept(buffer);
	}

	protected abstract B createBuffer();

}
