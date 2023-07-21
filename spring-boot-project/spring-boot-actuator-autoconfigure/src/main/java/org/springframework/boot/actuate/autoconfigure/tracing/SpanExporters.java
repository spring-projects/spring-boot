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

import io.opentelemetry.sdk.trace.export.SpanExporter;

import org.springframework.util.Assert;

/**
 * A collection of {@link SpanExporter span exporters}.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
@FunctionalInterface
public interface SpanExporters extends Iterable<SpanExporter> {

	/**
	 * Returns the list of {@link SpanExporter span exporters}.
	 * @return the list of span exporters
	 */
	List<SpanExporter> list();

	@Override
	default Iterator<SpanExporter> iterator() {
		return list().iterator();
	}

	@Override
	default Spliterator<SpanExporter> spliterator() {
		return list().spliterator();
	}

	/**
	 * Constructs a {@link SpanExporters} instance with the given {@link SpanExporter span
	 * exporters}.
	 * @param spanExporters the span exporters
	 * @return the constructed {@link SpanExporters} instance
	 */
	static SpanExporters of(SpanExporter... spanExporters) {
		return of(Arrays.asList(spanExporters));
	}

	/**
	 * Constructs a {@link SpanExporters} instance with the given list of
	 * {@link SpanExporter span exporters}.
	 * @param spanExporters the list of span exporters
	 * @return the constructed {@link SpanExporters} instance
	 */
	static SpanExporters of(Collection<? extends SpanExporter> spanExporters) {
		Assert.notNull(spanExporters, "SpanExporters must not be null");
		List<SpanExporter> copy = List.copyOf(spanExporters);
		return () -> copy;
	}

}
