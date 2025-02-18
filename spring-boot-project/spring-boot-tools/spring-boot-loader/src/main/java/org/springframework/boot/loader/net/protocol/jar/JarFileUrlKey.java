/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.net.protocol.jar;

import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to generate a string key from a jar file {@link URL} that can be used as a
 * cache key.
 *
 * @author Phillip Webb
 */
final class JarFileUrlKey {

	private static volatile SoftReference<Map<URL, String>> cache;

	private JarFileUrlKey() {
	}

	/**
	 * Get the {@link JarFileUrlKey} for the given URL.
	 * @param url the source URL
	 * @return a {@link JarFileUrlKey} instance
	 */
	static String get(URL url) {
		Map<URL, String> cache = (JarFileUrlKey.cache != null) ? JarFileUrlKey.cache.get() : null;
		if (cache == null) {
			cache = new ConcurrentHashMap<>();
			JarFileUrlKey.cache = new SoftReference<>(cache);
		}
		return cache.computeIfAbsent(url, JarFileUrlKey::create);
	}

	private static String create(URL url) {
		StringBuilder value = new StringBuilder();
		String protocol = url.getProtocol();
		String host = url.getHost();
		int port = (url.getPort() != -1) ? url.getPort() : url.getDefaultPort();
		String file = url.getFile();
		value.append(protocol.toLowerCase(Locale.ROOT));
		value.append(":");
		if (host != null && !host.isEmpty()) {
			value.append(host.toLowerCase(Locale.ROOT));
			value.append((port != -1) ? ":" + port : "");
		}
		value.append((file != null) ? file : "");
		if ("runtime".equals(url.getRef())) {
			value.append("#runtime");
		}
		return value.toString();
	}

	static void clearCache() {
		cache = null;
	}

}
