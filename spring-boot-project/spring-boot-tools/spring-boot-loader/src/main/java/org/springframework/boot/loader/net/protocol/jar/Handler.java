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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * {@link URLStreamHandler} alternative to {@code sun.net.www.protocol.jar.Handler} with
 * optimized support for nested jars.
 *
 * @author Phillip Webb
 * @since 3.2.0
 * @see org.springframework.boot.loader.net.protocol.Handlers
 */
public class Handler extends URLStreamHandler {

	// NOTE: in order to be found as a URL protocol handler, this class must be public,
	// must be named Handler and must be in a package ending '.jar'

	private static final String PROTOCOL = "jar";

	private static final String SEPARATOR = "!/";

	static final Handler INSTANCE = new Handler();

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return JarUrlConnection.open(url);
	}

	@Override
	protected void parseURL(URL url, String spec, int start, int limit) {
		if (spec.regionMatches(true, start, "jar:", 0, 4)) {
			throw new IllegalStateException("Nested JAR URLs are not supported");
		}
		int anchorIndex = spec.indexOf('#', limit);
		String path = extractPath(url, spec, start, limit, anchorIndex);
		String ref = (anchorIndex != -1) ? spec.substring(anchorIndex + 1) : null;
		setURL(url, PROTOCOL, "", -1, null, null, path, null, ref);
	}

	private String extractPath(URL url, String spec, int start, int limit, int anchorIndex) {
		if (anchorIndex == start) {
			return extractAnchorOnlyPath(url);
		}
		if (spec.length() >= 4 && spec.regionMatches(true, 0, "jar:", 0, 4)) {
			return extractAbsolutePath(spec, start, limit);
		}
		return extractRelativePath(url, spec, start, limit);
	}

	private String extractAnchorOnlyPath(URL url) {
		return url.getPath();
	}

	private String extractAbsolutePath(String spec, int start, int limit) {
		int indexOfSeparator = indexOfSeparator(spec, start, limit);
		if (indexOfSeparator == -1) {
			throw new IllegalStateException("no !/ in spec");
		}
		String innerUrl = spec.substring(start, indexOfSeparator);
		assertInnerUrlIsNotMalformed(spec, innerUrl);
		return spec.substring(start, limit);
	}

	private String extractRelativePath(URL url, String spec, int start, int limit) {
		String contextPath = extractContextPath(url, spec, start);
		String path = contextPath + spec.substring(start, limit);
		return Canonicalizer.canonicalizeAfter(path, indexOfSeparator(path) + 1);
	}

	private String extractContextPath(URL url, String spec, int start) {
		String contextPath = url.getPath();
		if (spec.charAt(start) == '/') {
			int indexOfContextPathSeparator = indexOfSeparator(contextPath);
			if (indexOfContextPathSeparator == -1) {
				throw new IllegalStateException("malformed context url:%s: no !/".formatted(url));
			}
			return contextPath.substring(0, indexOfContextPathSeparator + 1);
		}
		int lastSlash = contextPath.lastIndexOf('/');
		if (lastSlash == -1) {
			throw new IllegalStateException("malformed context url:%s".formatted(url));
		}
		return contextPath.substring(0, lastSlash + 1);
	}

	private void assertInnerUrlIsNotMalformed(String spec, String innerUrl) {
		if (innerUrl.startsWith("nested:")) {
			org.springframework.boot.loader.net.protocol.nested.Handler.assertUrlIsNotMalformed(innerUrl);
			return;
		}
		try {
			new URL(innerUrl);
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("invalid url: %s (%s)".formatted(spec, ex));
		}
	}

	@Override
	protected int hashCode(URL url) {
		String protocol = url.getProtocol();
		int hash = (protocol != null) ? protocol.hashCode() : 0;
		String file = url.getFile();
		int indexOfSeparator = file.indexOf(SEPARATOR);
		if (indexOfSeparator == -1) {
			return hash + file.hashCode();
		}
		String fileWithoutEntry = file.substring(0, indexOfSeparator);
		try {
			hash += new URL(fileWithoutEntry).hashCode();
		}
		catch (MalformedURLException ex) {
			hash += fileWithoutEntry.hashCode();
		}
		String entry = file.substring(indexOfSeparator + 2);
		return hash + entry.hashCode();
	}

	@Override
	protected boolean sameFile(URL url1, URL url2) {
		if (!url1.getProtocol().equals(PROTOCOL) || !url2.getProtocol().equals(PROTOCOL)) {
			return false;
		}
		String file1 = url1.getFile();
		String file2 = url2.getFile();
		int indexOfSeparator1 = file1.indexOf(SEPARATOR);
		int indexOfSeparator2 = file2.indexOf(SEPARATOR);
		if (indexOfSeparator1 == -1 || indexOfSeparator2 == -1) {
			return super.sameFile(url1, url2);
		}
		String entry1 = file1.substring(indexOfSeparator1 + 2);
		String entry2 = file2.substring(indexOfSeparator2 + 2);
		if (!entry1.equals(entry2)) {
			return false;
		}
		try {
			URL innerUrl1 = new URL(file1.substring(0, indexOfSeparator1));
			URL innerUrl2 = new URL(file2.substring(0, indexOfSeparator2));
			if (!super.sameFile(innerUrl1, innerUrl2)) {
				return false;
			}
		}
		catch (MalformedURLException unused) {
			return super.sameFile(url1, url2);
		}
		return true;
	}

	static int indexOfSeparator(String spec) {
		return indexOfSeparator(spec, 0, spec.length());
	}

	static int indexOfSeparator(String spec, int start, int limit) {
		for (int i = limit - 1; i >= start; i--) {
			if (spec.charAt(i) == '!' && (i + 1) < limit && spec.charAt(i + 1) == '/') {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Clear any internal caches.
	 */
	public static void clearCache() {
		JarFileUrlKey.clearCache();
		JarUrlConnection.clearCache();
	}

}
