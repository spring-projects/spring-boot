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

package org.springframework.boot.actuate.metrics.cache;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;

import org.springframework.data.redis.cache.RedisCache;

/**
 * {@link CacheMeterBinder} for {@link RedisCache}.
 *
 * @author Stephane Nicoll
 * @since 2.4.0
 */
public class RedisCacheMetrics extends CacheMeterBinder<RedisCache> {

	private final RedisCache cache;

	/**
	 * Constructs a new RedisCacheMetrics object with the specified RedisCache and tags.
	 * @param cache the RedisCache object to monitor metrics for
	 * @param tags the tags to associate with the metrics
	 */
	public RedisCacheMetrics(RedisCache cache, Iterable<Tag> tags) {
		super(cache, cache.getName(), tags);
		this.cache = cache;
	}

	/**
	 * Returns the size of the Redis cache.
	 * @return the size of the Redis cache, which is always null in this implementation.
	 */
	@Override
	protected Long size() {
		return null;
	}

	/**
	 * Returns the number of cache hits.
	 * @return the number of cache hits
	 */
	@Override
	protected long hitCount() {
		return this.cache.getStatistics().getHits();
	}

	/**
	 * Returns the number of cache misses.
	 * @return the number of cache misses
	 */
	@Override
	protected Long missCount() {
		return this.cache.getStatistics().getMisses();
	}

	/**
	 * Returns the count of evictions from the Redis cache.
	 * @return the count of evictions, or null if not available
	 */
	@Override
	protected Long evictionCount() {
		return null;
	}

	/**
	 * Returns the number of cache puts made by the RedisCacheMetrics instance.
	 * @return the number of cache puts made
	 */
	@Override
	protected long putCount() {
		return this.cache.getStatistics().getPuts();
	}

	/**
	 * Binds implementation-specific metrics for the RedisCacheMetrics class.
	 * @param registry The MeterRegistry to bind the metrics to.
	 */
	@Override
	protected void bindImplementationSpecificMetrics(MeterRegistry registry) {
		FunctionCounter.builder("cache.removals", this.cache, (cache) -> cache.getStatistics().getDeletes())
			.tags(getTagsWithCacheName())
			.description("Cache removals")
			.register(registry);
		FunctionCounter.builder("cache.gets", this.cache, (cache) -> cache.getStatistics().getPending())
			.tags(getTagsWithCacheName())
			.tag("result", "pending")
			.description("The number of pending requests")
			.register(registry);
		TimeGauge
			.builder("cache.lock.duration", this.cache, TimeUnit.NANOSECONDS,
					(cache) -> cache.getStatistics().getLockWaitDuration(TimeUnit.NANOSECONDS))
			.tags(getTagsWithCacheName())
			.description("The time the cache has spent waiting on a lock")
			.register(registry);
	}

}
