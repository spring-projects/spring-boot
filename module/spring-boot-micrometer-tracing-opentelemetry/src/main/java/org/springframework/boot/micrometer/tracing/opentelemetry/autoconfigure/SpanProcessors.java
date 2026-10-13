/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

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
 * @since 4.0.0
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
		Assert.notNull(spanProcessors, "'spanProcessors' must not be null");
		List<SpanProcessor> copy = List.copyOf(spanProcessors);
		return () -> copy;
	}

}
