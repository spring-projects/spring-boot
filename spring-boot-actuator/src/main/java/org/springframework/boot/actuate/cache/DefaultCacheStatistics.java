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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.boot.actuate.metrics.Metric;

/**
 * A default {@link CacheStatistics} implementation.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class DefaultCacheStatistics implements CacheStatistics {

	private Long size;

	private Double hitRatio;

	private Double missRatio;

	@Override
	public Collection<Metric<?>> toMetrics(String prefix) {
		Collection<Metric<?>> result = new ArrayList<Metric<?>>();
		addMetric(result, prefix + "size", getSize());
		addMetric(result, prefix + "hit.ratio", getHitRatio());
		addMetric(result, prefix + "miss.ratio", getMissRatio());
		return result;
	}

	public void setGetCacheCounts(long hitCount, long missCount) {
		long total = hitCount + missCount;
		if (total > 0) {
			double hitRatio = hitCount / (double) total;
			setHitRatio(hitRatio);
			setMissRatio(1 - hitRatio);
		}
	}

	@Override
	public Long getSize() {
		return this.size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	@Override
	public Double getHitRatio() {
		return this.hitRatio;
	}

	public void setHitRatio(Double hitRatio) {
		this.hitRatio = hitRatio;
	}

	@Override
	public Double getMissRatio() {
		return this.missRatio;
	}

	public void setMissRatio(Double missRatio) {
		this.missRatio = missRatio;
	}

	private <T extends Number> void addMetric(Collection<Metric<?>> metrics, String name,
			T value) {
		if (value != null) {
			metrics.add(new Metric<T>(name, value));
		}
	}
}
