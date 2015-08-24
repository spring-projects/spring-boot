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
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.cache.CacheStatistics;
import org.springframework.boot.actuate.cache.CacheStatisticsProvider;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.ResolvableType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A {@link PublicMetrics} implementation that provides cache statistics.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class CachePublicMetrics implements PublicMetrics {

	@Autowired
	private Map<String, CacheManager> cacheManagers;

	@Autowired
	private Collection<CacheStatisticsProvider<?>> statisticsProviders;

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> metrics = new HashSet<Metric<?>>();
		for (Map.Entry<String, List<CacheManagerBean>> entry : getCacheManagerBeans()
				.entrySet()) {
			addMetrics(metrics, entry.getKey(), entry.getValue());
		}
		return metrics;
	}

	private MultiValueMap<String, CacheManagerBean> getCacheManagerBeans() {
		MultiValueMap<String, CacheManagerBean> cacheManagerNamesByCacheName = new LinkedMultiValueMap<String, CacheManagerBean>();
		for (Map.Entry<String, CacheManager> entry : this.cacheManagers.entrySet()) {
			for (String cacheName : entry.getValue().getCacheNames()) {
				cacheManagerNamesByCacheName.add(cacheName,
						new CacheManagerBean(entry.getKey(), entry.getValue()));
			}
		}
		return cacheManagerNamesByCacheName;
	}

	private void addMetrics(Collection<Metric<?>> metrics, String cacheName,
			List<CacheManagerBean> cacheManagerBeans) {
		for (CacheManagerBean cacheManagerBean : cacheManagerBeans) {
			CacheManager cacheManager = cacheManagerBean.getCacheManager();
			Cache cache = cacheManager.getCache(cacheName);
			CacheStatistics statistics = getCacheStatistics(cache, cacheManager);
			if (statistics != null) {
				String prefix = cacheName;
				if (cacheManagerBeans.size() > 1) {
					prefix = cacheManagerBean.getBeanName() + "_" + prefix;
				}
				prefix = "cache." + prefix + (prefix.endsWith(".") ? "" : ".");
				metrics.addAll(statistics.toMetrics(prefix));
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private CacheStatistics getCacheStatistics(Cache cache, CacheManager cacheManager) {
		if (this.statisticsProviders != null) {
			for (CacheStatisticsProvider provider : this.statisticsProviders) {
				Class<?> cacheType = ResolvableType.forClass(
						CacheStatisticsProvider.class, provider.getClass())
						.resolveGeneric();
				if (cacheType.isInstance(cache)) {
					CacheStatistics statistics = provider.getCacheStatistics(
							cacheManager, cache);
					if (statistics != null) {
						return statistics;
					}
				}
			}
		}
		return null;
	}

	private static class CacheManagerBean {

		private final String beanName;

		private final CacheManager cacheManager;

		public CacheManagerBean(String beanName, CacheManager cacheManager) {
			this.beanName = beanName;
			this.cacheManager = cacheManager;
		}

		public String getBeanName() {
			return this.beanName;
		}

		public CacheManager getCacheManager() {
			return this.cacheManager;
		}

	}

}
