/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;

import io.opentelemetry.sdk.trace.SpanProcessor;

import org.springframework.util.Assert;

/**
 * A collection of {@link SpanProcessor span processors}.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
@FunctionalInterface
public interface SpanProcessors extends Iterable<SpanProcessor> {

	/**
	 * Returns the list of {@link SpanProcessor span processors}.
	 * @return the list of span processors
	 */
	List<SpanProcessor> list();

	@Override
	default Iterator<SpanProcessor> iterator() {
		return list().iterator();
	}

	@Override
	default Spliterator<SpanProcessor> spliterator() {
		return list().spliterator();
	}

	/**
	 * Constructs a {@link SpanProcessors} instance with the given {@link SpanProcessor
	 * span processors}.
	 * @param spanProcessors the span processors
	 * @return the constructed {@link SpanProcessors} instance
	 */
	static SpanProcessors of(SpanProcessor... spanProcessors) {
		return of(Arrays.asList(spanProcessors));
	}

	/**
	 * Constructs a {@link SpanProcessors} instance with the given list of
	 * {@link SpanProcessor span processors}.
	 * @param spanProcessors the list of span processors
	 * @return the constructed {@link SpanProcessors} instance
	 */
	static SpanProcessors of(Collection<? extends SpanProcessor> spanProcessors) {
		Assert.notNull(spanProcessors, "SpanProcessors must not be null");
		List<SpanProcessor> copy = List.copyOf(spanProcessors);
		return () -> copy;
	}

}
