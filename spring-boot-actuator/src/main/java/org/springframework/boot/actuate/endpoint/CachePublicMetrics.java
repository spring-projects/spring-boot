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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.cache.CacheStatistics;
import org.springframework.boot.actuate.cache.CacheStatisticsProvider;
import org.springframework.boot.actuate.cache.CacheStatisticsProviders;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * A {@link PublicMetrics} implementation that provides cache statistics.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class CachePublicMetrics implements PublicMetrics {

	@Autowired
	private Collection<CacheStatisticsProvider> providers;

	@Autowired
	private Map<String, CacheManager> cacheManagers;

	private CacheStatisticsProvider cacheStatisticsProvider;

	@PostConstruct
	public void initialize() {
		this.cacheStatisticsProvider = new CacheStatisticsProviders(this.providers);
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Set<String> targetNames = new HashSet<String>();
		Collection<Metric<?>> metrics = new HashSet<Metric<?>>();
		for (Map.Entry<String, CacheManager> entry : this.cacheManagers.entrySet()) {
			String cacheManagerName = entry.getKey();
			CacheManager cacheManager = entry.getValue();
			for (String cacheName : cacheManager.getCacheNames()) {
				Cache cache = cacheManager.getCache(cacheName);
				CacheStatistics cacheStatistics = this.cacheStatisticsProvider
						.getCacheStatistics(cache, cacheManager);
				if (cacheStatistics != null) {
					String prefix = cleanPrefix(createPrefix(targetNames, cacheName,
							cacheManagerName));
					metrics.addAll(cacheStatistics.toMetrics(prefix));
				}
			}
		}
		return metrics;
	}

	/**
	 * Create the prefix to use for the specified cache. The specified {@code targetNames}
	 * set contains the names that have been acquired so far.
	 * @param targetNames the target names that have been used for other caches
	 * @param cacheName the name of the cache
	 * @param cacheManagerName the name of its cache manager
	 * @return a prefix to use for the specified cache
	 */
	protected String createPrefix(Set<String> targetNames, String cacheName,
			String cacheManagerName) {
		if (targetNames.contains(cacheName)) {
			String target = cacheManagerName + "_" + cacheName;
			return createPrefixFor(target);
		}
		else {
			targetNames.add(cacheName);
			return createPrefixFor(cacheName);
		}
	}

	protected String createPrefixFor(String name) {
		return "cache." + name;
	}

	private String cleanPrefix(String prefix) {
		return (prefix.endsWith(".") ? prefix : prefix + ".");
	}

}
