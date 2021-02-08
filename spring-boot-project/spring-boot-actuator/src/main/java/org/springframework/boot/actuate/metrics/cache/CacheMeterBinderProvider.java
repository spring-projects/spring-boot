/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.metrics.cache;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.cache.Cache;

/**
 * Provide a {@link MeterBinder} based on a {@link Cache}.
 *
 * @param <C> the cache type
 * @author Stephane Nicoll
 * @since 2.0.0
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
	MeterBinder getMeterBinder(C cache, Iterable<Tag> tags);

}
