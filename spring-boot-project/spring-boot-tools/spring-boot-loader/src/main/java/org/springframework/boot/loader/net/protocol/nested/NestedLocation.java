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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.loader.net.util.UrlDecoder;

/**
 * A location obtained from a {@code nested:} {@link URL} consisting of a jar file and an
 * optional nested entry.
 * <p>
 * The syntax of a nested JAR URL is: <pre>
 * nestedjar:&lt;path&gt;/!{entry}
 * </pre>
 * <p>
 * for example:
 * <p>
 * {@code nested:/home/example/my.jar/!BOOT-INF/lib/my-nested.jar}
 * <p>
 * or:
 * <p>
 * {@code nested:/home/example/my.jar/!BOOT-INF/classes/}
 * <p>
 * The path must refer to a jar file on the file system. The entry refers to either an
 * uncompressed entry that contains the nested jar, or a directory entry. The entry must
 * not start with a {@code '/'}.
 *
 * @param path the path to the zip that contains the nested entry
 * @param nestedEntryName the nested entry name
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public record NestedLocation(Path path, String nestedEntryName) {

	private static final Map<String, NestedLocation> locationCache = new ConcurrentHashMap<>();

	private static final Map<String, Path> pathCache = new ConcurrentHashMap<>();

	public NestedLocation(Path path, String nestedEntryName) {
		if (path == null) {
			throw new IllegalArgumentException("'path' must not be null");
		}
		this.path = path;
		this.nestedEntryName = (nestedEntryName != null && !nestedEntryName.isEmpty()) ? nestedEntryName : null;
	}

	/**
	 * Create a new {@link NestedLocation} from the given URL.
	 * @param url the nested URL
	 * @return a new {@link NestedLocation} instance
	 * @throws IllegalArgumentException if the URL is not valid
	 */
	public static NestedLocation fromUrl(URL url) {
		if (url == null || !"nested".equalsIgnoreCase(url.getProtocol())) {
			throw new IllegalArgumentException("'url' must not be null and must use 'nested' protocol");
		}
		return parse(UrlDecoder.decode(url.toString().substring(7)));
	}

	/**
	 * Create a new {@link NestedLocation} from the given URI.
	 * @param uri the nested URI
	 * @return a new {@link NestedLocation} instance
	 * @throws IllegalArgumentException if the URI is not valid
	 */
	public static NestedLocation fromUri(URI uri) {
		if (uri == null || !"nested".equalsIgnoreCase(uri.getScheme())) {
			throw new IllegalArgumentException("'uri' must not be null and must use 'nested' scheme");
		}
		return parse(uri.getSchemeSpecificPart());
	}

	static NestedLocation parse(String location) {
		if (location == null || location.isEmpty()) {
			throw new IllegalArgumentException("'location' must not be empty");
		}
		return locationCache.computeIfAbsent(location, (key) -> create(location));
	}

	private static NestedLocation create(String location) {
		int index = location.lastIndexOf("/!");
		String locationPath = (index != -1) ? location.substring(0, index) : location;
		String nestedEntryName = (index != -1) ? location.substring(index + 2) : null;
		return new NestedLocation((!locationPath.isEmpty()) ? asPath(locationPath) : null, nestedEntryName);
	}

	private static Path asPath(String locationPath) {
		return pathCache.computeIfAbsent(locationPath,
				(key) -> Path.of((!isWindows()) ? locationPath : fixWindowsLocationPath(locationPath)));
	}

	private static boolean isWindows() {
		return File.separatorChar == '\\';
	}

	private static String fixWindowsLocationPath(String locationPath) {
		// Same logic as Java's internal WindowsUriSupport class
		if (locationPath.length() > 2 && locationPath.charAt(2) == ':') {
			return locationPath.substring(1);
		}
		// Deal with Jetty's org.eclipse.jetty.util.URIUtil#correctURI(URI)
		if (locationPath.startsWith("///") && locationPath.charAt(4) == ':') {
			return locationPath.substring(3);
		}
		return locationPath;
	}

	static void clearCache() {
		locationCache.clear();
		pathCache.clear();
	}

}
