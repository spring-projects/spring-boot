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

package org.springframework.boot.actuate.cache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * {@link Endpoint @Endpoint} to expose available {@link Cache caches}.
 *
 * @author Johannes Edmeier
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
	 * Return a {@link CachesDescriptor} of all available {@link Cache caches}.
	 * @return a caches reports
	 */
	@ReadOperation
	public CachesDescriptor caches() {
		Map<String, Map<String, CacheDescriptor>> descriptors = new LinkedHashMap<>();
		getCacheEntries(matchAll(), matchAll()).forEach((entry) -> {
			String cacheName = entry.getName();
			String cacheManager = entry.getCacheManager();
			Map<String, CacheDescriptor> cacheManagerDescriptors = descriptors.computeIfAbsent(cacheManager,
					(key) -> new LinkedHashMap<>());
			cacheManagerDescriptors.put(cacheName, new CacheDescriptor(entry.getTarget()));
		});
		Map<String, CacheManagerDescriptor> cacheManagerDescriptors = new LinkedHashMap<>();
		descriptors.forEach((name, entries) -> cacheManagerDescriptors.put(name, new CacheManagerDescriptor(entries)));
		return new CachesDescriptor(cacheManagerDescriptors);
	}

	/**
	 * Return a {@link CacheDescriptor} for the specified cache.
	 * @param cache the name of the cache
	 * @param cacheManager the name of the cacheManager (can be {@code null}
	 * @return the descriptor of the cache or {@code null} if no such cache exists
	 * @throws NonUniqueCacheException if more than one cache with that name exists and no
	 * {@code cacheManager} was provided to identify a unique candidate
	 */
	@ReadOperation
	public CacheEntryDescriptor cache(@Selector String cache, @Nullable String cacheManager) {
		return extractUniqueCacheEntry(cache, getCacheEntries((name) -> name.equals(cache), isNameMatch(cacheManager)));
	}

	/**
	 * Clear all the available {@link Cache caches}.
	 */
	@DeleteOperation
	public void clearCaches() {
		getCacheEntries(matchAll(), matchAll()).forEach(this::clearCache);
	}

	/**
	 * Clear the specific {@link Cache}.
	 * @param cache the name of the cache
	 * @param cacheManager the name of the cacheManager (can be {@code null} to match all)
	 * @return {@code true} if the cache was cleared or {@code false} if no such cache
	 * exists
	 * @throws NonUniqueCacheException if more than one cache with that name exists and no
	 * {@code cacheManager} was provided to identify a unique candidate
	 */
	@DeleteOperation
	public boolean clearCache(@Selector String cache, @Nullable String cacheManager) {
		CacheEntryDescriptor entry = extractUniqueCacheEntry(cache,
				getCacheEntries((name) -> name.equals(cache), isNameMatch(cacheManager)));
		return (entry != null && clearCache(entry));
	}

	/**
     * Retrieves a list of cache entries based on the provided cache name and cache manager name predicates.
     * 
     * @param cacheNamePredicate A predicate used to filter cache names.
     * @param cacheManagerNamePredicate A predicate used to filter cache manager names.
     * @return A list of CacheEntryDescriptor objects representing the cache entries that match the provided predicates.
     */
    private List<CacheEntryDescriptor> getCacheEntries(Predicate<String> cacheNamePredicate,
			Predicate<String> cacheManagerNamePredicate) {
		return this.cacheManagers.keySet()
			.stream()
			.filter(cacheManagerNamePredicate)
			.flatMap((cacheManagerName) -> getCacheEntries(cacheManagerName, cacheNamePredicate).stream())
			.toList();
	}

	/**
     * Retrieves a list of cache entries based on the provided cache manager name and cache name predicate.
     * 
     * @param cacheManagerName the name of the cache manager
     * @param cacheNamePredicate the predicate used to filter cache names
     * @return a list of CacheEntryDescriptor objects representing the cache entries
     */
    private List<CacheEntryDescriptor> getCacheEntries(String cacheManagerName, Predicate<String> cacheNamePredicate) {
		CacheManager cacheManager = this.cacheManagers.get(cacheManagerName);
		return cacheManager.getCacheNames()
			.stream()
			.filter(cacheNamePredicate)
			.map(cacheManager::getCache)
			.filter(Objects::nonNull)
			.map((cache) -> new CacheEntryDescriptor(cache, cacheManagerName))
			.toList();
	}

	/**
     * Extracts a unique cache entry from the given list of cache entries.
     * 
     * @param cache   the name of the cache
     * @param entries the list of cache entry descriptors
     * @return the unique cache entry descriptor, or null if the list is empty
     * @throws NonUniqueCacheException if the list contains more than one cache entry with different cache managers
     */
    private CacheEntryDescriptor extractUniqueCacheEntry(String cache, List<CacheEntryDescriptor> entries) {
		if (entries.size() > 1) {
			throw new NonUniqueCacheException(cache,
					entries.stream().map(CacheEntryDescriptor::getCacheManager).distinct().toList());
		}
		return (!entries.isEmpty() ? entries.get(0) : null);
	}

	/**
     * Clears the cache specified by the given cache entry descriptor.
     * 
     * @param entry the cache entry descriptor containing the cache name and cache manager
     * @return true if the cache was successfully cleared, false otherwise
     */
    private boolean clearCache(CacheEntryDescriptor entry) {
		String cacheName = entry.getName();
		String cacheManager = entry.getCacheManager();
		Cache cache = this.cacheManagers.get(cacheManager).getCache(cacheName);
		if (cache != null) {
			cache.clear();
			return true;
		}
		return false;
	}

	/**
     * Returns a Predicate that checks if a given name matches the specified name.
     * 
     * @param name the name to match against
     * @return a Predicate that checks if a given name matches the specified name
     */
    private Predicate<String> isNameMatch(String name) {
		return (name != null) ? ((requested) -> requested.equals(name)) : matchAll();
	}

	/**
     * Returns a Predicate that matches all strings.
     *
     * @return a Predicate that matches all strings
     */
    private Predicate<String> matchAll() {
		return (name) -> true;
	}

	/**
	 * Description of the caches.
	 */
	public static final class CachesDescriptor implements OperationResponseBody {

		private final Map<String, CacheManagerDescriptor> cacheManagers;

		/**
         * Constructs a new CachesDescriptor object with the specified cache managers.
         * 
         * @param cacheManagers a Map containing the cache managers, where the key is the cache manager name and the value is the CacheManagerDescriptor object
         */
        public CachesDescriptor(Map<String, CacheManagerDescriptor> cacheManagers) {
			this.cacheManagers = cacheManagers;
		}

		/**
         * Returns a map of cache managers.
         * 
         * @return a map containing cache manager descriptors
         */
        public Map<String, CacheManagerDescriptor> getCacheManagers() {
			return this.cacheManagers;
		}

	}

	/**
	 * Description of a {@link CacheManager}.
	 */
	public static final class CacheManagerDescriptor {

		private final Map<String, CacheDescriptor> caches;

		/**
         * Constructs a new CacheManagerDescriptor with the specified caches.
         *
         * @param caches a map of cache names to cache descriptors
         */
        public CacheManagerDescriptor(Map<String, CacheDescriptor> caches) {
			this.caches = caches;
		}

		/**
         * Returns a map of caches in the cache manager.
         *
         * @return a map of caches, where the key is the cache name and the value is the cache descriptor
         */
        public Map<String, CacheDescriptor> getCaches() {
			return this.caches;
		}

	}

	/**
	 * Description of a {@link Cache}.
	 */
	public static class CacheDescriptor implements OperationResponseBody {

		private final String target;

		/**
         * Constructs a new CacheDescriptor with the specified target.
         *
         * @param target the target of the cache descriptor
         */
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
	 * Description of a {@link Cache} entry.
	 */
	public static final class CacheEntryDescriptor extends CacheDescriptor {

		private final String name;

		private final String cacheManager;

		/**
         * Constructs a new CacheEntryDescriptor object with the specified cache and cache manager.
         * 
         * @param cache the cache object to associate with the descriptor
         * @param cacheManager the name of the cache manager
         */
        public CacheEntryDescriptor(Cache cache, String cacheManager) {
			super(cache.getNativeCache().getClass().getName());
			this.name = cache.getName();
			this.cacheManager = cacheManager;
		}

		/**
         * Returns the name of the CacheEntryDescriptor.
         *
         * @return the name of the CacheEntryDescriptor
         */
        public String getName() {
			return this.name;
		}

		/**
         * Returns the cache manager associated with this CacheEntryDescriptor.
         *
         * @return the cache manager associated with this CacheEntryDescriptor
         */
        public String getCacheManager() {
			return this.cacheManager;
		}

	}

}
