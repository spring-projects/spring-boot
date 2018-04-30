/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * {@link Endpoint} to expose available {@link Cache caches}.
 *
 * @author Johannes Edmeuer
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Endpoint(id = "caches")
public class CachesEndpoint {

	private final Map<String, CacheManager> cacheManagers;

	/**
	 * Create a new endpoint with the {@link CacheManager} instances to use.
	 * @param cacheManagers the cache managers to use, indexed by name
	 */
	public CachesEndpoint(Map<String, CacheManager> cacheManagers) {
		this.cacheManagers = new LinkedHashMap<>(cacheManagers);
	}

	/**
	 * Return a {@link CachesReport} of all available {@link Cache caches}.
	 * @return a caches reports
	 */
	@ReadOperation
	public CachesReport caches() {
		Map<String, Map<String, CacheDescriptor>> descriptors = new LinkedHashMap<>();
		getCacheEntries((name) -> true, (cacheManager) -> true).forEach((entry) -> {
			Map<String, CacheDescriptor> cmDescriptors = descriptors.computeIfAbsent(
					entry.getCacheManager(), (key) -> new LinkedHashMap<>());
			String cache = entry.getName();
			cmDescriptors.put(cache, new CacheDescriptor(entry.getTarget()));
		});
		return new CachesReport(descriptors);
	}

	/**
	 * Return a {@link CacheDescriptor} for the specified cache.
	 * @param cache then name of the cache
	 * @param cacheManager the name of the cacheManager (can be {@code null}
	 * @return the descriptor of the cache or {@code null} if no such cache exists
	 * @throws NonUniqueCacheException if more than one cache with that name exist and no
	 * {@code cacheManager} was provided to identify a unique candidate
	 */
	@ReadOperation
	public CacheEntry cache(@Selector String cache, @Nullable String cacheManager) {
		return extractUniqueCacheEntry(cache, getCacheEntries(
				(name) -> name.equals(cache), safeEqual(cacheManager)));
	}

	/**
	 * Clear all the available {@link Cache caches}.
	 */
	@DeleteOperation
	public void clearCaches() {
		getCacheEntries((name) -> true, (cacheManagerName) -> true)
				.forEach(this::clearCache);
	}

	/**
	 * Clear the specific {@link Cache}.
	 * @param cache then name of the cache
	 * @param cacheManager the name of the cacheManager (can be {@code null}
	 * @return {@code true} if the cache was cleared or {@code false} if no such cache exists
	 * @throws NonUniqueCacheException if more than one cache with that name exist and no
	 */
	@DeleteOperation
	public boolean clearCache(@Selector String cache, @Nullable String cacheManager) {
		CacheEntry entry = extractUniqueCacheEntry(cache, getCacheEntries(
				(name) -> name.equals(cache), safeEqual(cacheManager)));
		return (entry != null && clearCache(entry));
	}

	private List<CacheEntry> getCacheEntries(
			Predicate<String> cacheNamePredicate,
			Predicate<String> cacheManagerNamePredicate) {
		List<CacheEntry> entries = new ArrayList<>();
		this.cacheManagers.keySet().stream().filter(cacheManagerNamePredicate)
				.forEach((cacheManagerName) -> entries.addAll(
						getCacheEntries(cacheManagerName, cacheNamePredicate)));
		return entries;
	}

	private List<CacheEntry> getCacheEntries(String cacheManagerName,
			Predicate<String> cacheNamePredicate) {
		CacheManager cacheManager = this.cacheManagers.get(cacheManagerName);
		List<CacheEntry> entries = new ArrayList<>();
		cacheManager.getCacheNames().stream().filter(cacheNamePredicate)
				.map(cacheManager::getCache).filter(Objects::nonNull)
				.forEach((cache) -> entries.add(
						new CacheEntry(cache, cacheManagerName)));
		return entries;
	}

	private CacheEntry extractUniqueCacheEntry(String cache,
			List<CacheEntry> entries) {
		if (entries.size() > 1) {
			throw new NonUniqueCacheException(cache, entries.stream()
					.map(CacheEntry::getCacheManager).distinct()
					.collect(Collectors.toList()));
		}
		return (entries.isEmpty() ? null : entries.get(0));
	}

	private boolean clearCache(CacheEntry entry) {
		String cacheName = entry.getName();
		Cache cache = this.cacheManagers.get(entry.getCacheManager()).getCache(cacheName);
		if (cache != null) {
			cache.clear();
			return true;
		}
		return false;
	}

	private Predicate<String> safeEqual(String name) {
		return (name != null ? ((requested) -> requested.equals(name))
				: ((requested) -> true));
	}

	/**
	 * A report of available {@link Cache caches}, primarily intended for serialization
	 * to JSON.
	 */
	public static final class CachesReport {

		private final Map<String, Map<String, CacheDescriptor>> cacheManagers;

		public CachesReport(Map<String, Map<String, CacheDescriptor>> cacheManagers) {
			this.cacheManagers = cacheManagers;
		}

		public Map<String, Map<String, CacheDescriptor>> getCacheManagers() {
			return this.cacheManagers;
		}

	}

	/**
	 * Basic description of a {@link Cache}, primarily intended for serialization to JSON.
	 */
	public static class CacheDescriptor {

		private final String target;

		public CacheDescriptor(String target) {
			this.target = target;
		}

		/**
		 * Return the fully qualified name of the native cache.
		 * @return the fully qualified name of the native cache
		 */
		public String getTarget() {
			return this.target;
		}

	}

	/**
	 * Description of a {@link Cache}, primarily intended for serialization to JSON.
	 */
	public static final class CacheEntry extends CacheDescriptor {

		private final String name;

		private final String cacheManager;

		public CacheEntry(Cache cache, String cacheManager) {
			super(cache.getNativeCache().getClass().getName());
			this.name = cache.getName();
			this.cacheManager = cacheManager;
		}

		public String getName() {
			return this.name;
		}

		public String getCacheManager() {
			return this.cacheManager;
		}

	}

}
