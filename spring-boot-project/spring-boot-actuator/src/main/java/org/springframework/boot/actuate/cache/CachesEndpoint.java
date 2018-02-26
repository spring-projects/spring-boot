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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * {@link Endpoint} to expose cache operations.
 *
 * @author Johannes Edmeuer
 * @since 2.0.0
 */
@Endpoint(id = "caches")
public class CachesEndpoint {
	private final ApplicationContext context;

	public CachesEndpoint(ApplicationContext context) {
		this.context = context;
	}

	@ReadOperation
	public ContextCacheManagerBeans cacheManagerBeans() {
		Map<String, CacheManagerDescriptor> cacheManagers = new HashMap<>();
		this.context.getBeansOfType(CacheManager.class)
				.forEach((name, cacheManager) -> cacheManagers.put(name,
						new CacheManagerDescriptor(cacheManager.getCacheNames())));
		return new ContextCacheManagerBeans(cacheManagers);
	}

	@DeleteOperation
	public void clearCaches(@Nullable String cacheManager, @Nullable String cacheName) {
		if (cacheManager == null) {
			this.context.getBeansOfType(CacheManager.class)
					.forEach((name, manager) -> this.clearCaches(manager, cacheName));
		}
		else {
			this.clearCaches(this.context.getBean(cacheManager, CacheManager.class),
					cacheName);
		}
	}

	private void clearCaches(CacheManager cacheManager, String cacheName) {
		if (cacheName == null) {
			cacheManager.getCacheNames().forEach(cn -> cacheManager.getCache(cn).clear());
		}
		else {
			cacheManager.getCache(cacheName).clear();
		}
	}

	/**
	 * Description of an application context's {@link CacheManager} beans, primarily
	 * intended for serialization to JSON.
	 */
	public static final class ContextCacheManagerBeans {

		private final Map<String, CacheManagerDescriptor> cacheManagers;

		private ContextCacheManagerBeans(
				Map<String, CacheManagerDescriptor> cacheManagers) {
			this.cacheManagers = cacheManagers;
		}

		public Map<String, CacheManagerDescriptor> getCacheManagers() {
			return this.cacheManagers;
		}
	}

	/**
	 * Description of a {@link CacheManager} bean, primarily intended for serialization to
	 * JSON.
	 */
	public static final class CacheManagerDescriptor {
		private final List<String> cacheNames;

		private CacheManagerDescriptor(Collection<String> cacheNames) {
			this.cacheNames = new ArrayList<>(cacheNames);
		}

		public List<String> getCacheNames() {
			return this.cacheNames;
		}
	}
}
