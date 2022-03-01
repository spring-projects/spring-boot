/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A class/resource cache used by {@link LaunchedURLClassLoader} .
 *
 * @author Bingjie Lv
 * @since 2.7.0
 *
 */
public class ClassLoaderCache {

	private boolean enableCache = Boolean.getBoolean("loader.cache.enable");

	private final int cacheSize = Integer.getInteger("loader.cache.size", 3000);

	private Map<String, ClassNotFoundException> classNotFoundExceptionCache;

	private Map<String, Optional<URL>> resourceUrlCache;

	private Map<String, Optional<Enumeration<URL>>> resourcesUrlCache;

	public ClassLoaderCache() {
		this.classNotFoundExceptionCache = createCache(this.cacheSize);
		this.resourceUrlCache = createCache(this.cacheSize);
		this.resourcesUrlCache = createCache(this.cacheSize);
	}

	public void fastClassNotFoundException(String name) throws ClassNotFoundException {
		if (!this.enableCache) {
			return;
		}
		ClassNotFoundException classNotFoundException = this.classNotFoundExceptionCache.get(name);
		if (classNotFoundException != null) {
			throw classNotFoundException;
		}
	}

	public void cacheClassNotFoundException(String name, ClassNotFoundException exception) {
		if (!this.enableCache) {
			return;
		}
		this.classNotFoundExceptionCache.put(name, exception);
	}

	public Optional<URL> getResourceCache(String name) {
		if (!this.enableCache) {
			return null;
		}
		return this.resourceUrlCache.get(name);
	}

	public URL cacheResourceUrl(String name, URL url) {
		if (!this.enableCache) {
			return url;
		}
		this.resourceUrlCache.put(name, (url != null) ? Optional.of(url) : Optional.empty());
		return url;
	}

	public Optional<Enumeration<URL>> getResourcesCache(String name) {
		if (!this.enableCache) {
			return null;
		}
		return this.resourcesUrlCache.get(name);
	}

	public Enumeration<URL> cacheResourceUrls(String name, Enumeration<URL> urlEnumeration) {
		if (!this.enableCache) {
			return urlEnumeration;
		}
		if (!urlEnumeration.hasMoreElements()) {
			this.resourcesUrlCache.put(name, Optional.of(urlEnumeration));
		}
		return urlEnumeration;
	}

	public void clearCache() {
		if (this.enableCache) {
			this.classNotFoundExceptionCache.clear();
			this.resourceUrlCache.clear();
			this.resourcesUrlCache.clear();
		}
	}

	public void setEnableCache(boolean enableCache) {
		this.enableCache = enableCache;
	}

	protected <K, V> Map<K, V> createCache(int maxSize) {
		return Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() >= maxSize;
			}
		});
	}

}
