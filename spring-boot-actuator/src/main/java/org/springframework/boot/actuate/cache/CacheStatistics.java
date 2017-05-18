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

package org.springframework.boot.actuate.cache;

import java.util.Collection;

import org.springframework.boot.actuate.metrics.Metric;

/**
 * Snapshot of the statistics of a given cache. {@code CacheStatistics} instances have a
 * very short life as it represents the statistics of a cache at one particular point in
 * time.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public interface CacheStatistics {

	/**
	 * Generate the relevant {@link Metric} instances based on the specified prefix.
	 * @param prefix the metrics prefix (ends with '.')
	 * @return the metrics corresponding to this instance
	 */
	Collection<Metric<?>> toMetrics(String prefix);

	/**
	 * Return the size of the cache or {@code null} if that information is not available.
	 * @return the size of the cache or {@code null}
	 */
	Long getSize();

	/**
	 * Return the ratio of cache requests which were hits as a value between 0 and 1 where
	 * 0 means that the hit ratio is 0% and 1 means it is 100%.
	 * <p>
	 * This may also return {@code null} if the cache-specifics statistics does not
	 * provide the necessary information
	 * @return the hit ratio or {@code null}
	 */
	Double getHitRatio();

	/**
	 * Return the ratio of cache requests which were misses as value between 0 and 1 where
	 * 0 means that the miss ratio is 0% and 1 means it is 100%.
	 * <p>
	 * This may also return {@code null} if the cache-specifics statistics does not
	 * provide the necessary information
	 * @return the miss ratio or {@code null}
	 */
	Double getMissRatio();

}
