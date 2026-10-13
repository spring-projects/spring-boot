/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.nested;

import java.io.IOException;
import java.net.InetAddress;
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
	protected InetAddress getHostAddress(URL url) {
		// Some Windows users have reported that calls to java.net.URL.getHostAddress()
		// can be slow. Since we only deal with local files we always return null here.
		return null;
	}

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
