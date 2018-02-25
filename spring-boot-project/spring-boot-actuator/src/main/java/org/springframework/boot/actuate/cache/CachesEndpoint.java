/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.List;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import static java.util.stream.Collectors.toList;

/**
 * {@link Endpoint} to expose cache operations.
 *
 * @author Johannes Edmeuer
 * @since 2.0.2
 */
@Endpoint(id = "caches")
public class CachesEndpoint {
	private final ApplicationContext context;

	public CachesEndpoint(ApplicationContext context) {
		this.context = context;
	}

	@ReadOperation
	public CachesDescriptor caches() {
		List<CacheDescriptor> caches = new ArrayList<>();
		this.context.getBeansOfType(CacheManager.class)
					.forEach((name, cacheManager) -> caches.addAll(
							getCacheDescriptors(name, cacheManager.getCacheNames())));
		return new CachesDescriptor(caches);
	}

	private Collection<? extends CacheDescriptor> getCacheDescriptors(String cacheManager,
																	  Collection<String> cacheNames) {
		return cacheNames.stream().map(cacheName -> new CacheDescriptor(cacheName, cacheManager)).collect(toList());
	}

	@DeleteOperation
	public void clearCaches(@Nullable String cacheManager, @Nullable String cacheName) {
		if (cacheManager == null) {
			this.context.getBeansOfType(CacheManager.class)
						.forEach((name, manager) -> this.clearCaches(manager, cacheName));
		} else {
			this.clearCaches(this.context.getBean(cacheManager, CacheManager.class), cacheName);
		}
	}

	private void clearCaches(CacheManager cacheManager, String cacheName) {
		if (cacheName == null) {
			cacheManager.getCacheNames().forEach(cn -> cacheManager.getCache(cn).clear());
		} else {
			Cache cache = cacheManager.getCache(cacheName);
			if (cache != null) {
				cache.clear();
			}
		}
	}

	/**
	 * Description of an application context's caches, primarily
	 * intended for serialization to JSON.
	 */
	public static final class CachesDescriptor {
		private final List<CacheDescriptor> caches;

		private CachesDescriptor(List<CacheDescriptor> caches) {
			this.caches = caches;
		}

		public List<CacheDescriptor> getCaches() {
			return this.caches;
		}
	}

	/**
	 * Description of a {@link Cache}, primarily intended for serialization to
	 * JSON.
	 */
	public static final class CacheDescriptor {
		private final String name;
		private final String cacheManager;

		public CacheDescriptor(String name, String cacheManager) {
			this.name = name;
			this.cacheManager = cacheManager;
		}

		public String getName() {
			return name;
		}

		public String getCacheManager() {
			return cacheManager;
		}
	}
}
