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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * {@link URLStreamHandler} to support {@code nested:} URLs. See {@link NestedLocation}
 * for details of the URL format.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public class Handler extends URLStreamHandler {

	// NOTE: in order to be found as a URL protocol handler, this class must be public,
	// must be named Handler and must be in a package ending '.nested'

	private static final String PREFIX = "nested:";

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new NestedUrlConnection(url);
	}

	/**
	 * Assert that the specified URL is a valid "nested" URL.
	 * @param url the URL to check
	 */
	public static void assertUrlIsNotMalformed(String url) {
		if (url == null || !url.startsWith(PREFIX)) {
			throw new IllegalArgumentException("'url' must not be null and must use 'nested' protocol");
		}
		NestedLocation.parse(url.substring(PREFIX.length()));
	}

	/**
	 * Clear any internal caches.
	 */
	public static void clearCache() {
		NestedLocation.clearCache();
	}

}
