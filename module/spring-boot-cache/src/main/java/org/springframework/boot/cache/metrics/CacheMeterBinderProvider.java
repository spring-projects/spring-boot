/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;

/**
 * Provide a {@link MeterBinder} based on a {@link Cache}.
 *
 * @param <C> the cache type
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface CacheMeterBinderProvider<C extends Cache> {

	/**
	 * Return the {@link MeterBinder} managing the specified {@link Cache} or {@code null}
	 * if the specified {@link Cache} is not supported.
	 * @param cache the cache to instrument
	 * @param tags tags to apply to all recorded metrics
	 * @return a {@link MeterBinder} handling the specified {@link Cache} or {@code null}
	 */
	@Nullable MeterBinder getMeterBinder(C cache, Iterable<Tag> tags);

}
