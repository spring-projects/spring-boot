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

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.loader.net.util.UrlDecoder;

/**
 * A location obtained from a {@code nested:} {@link URL} consisting of a jar file and a
 * nested entry.
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
 * @param file the zip file that contains the nested entry
 * @param nestedEntryName the nested entry name
 * @author Phillip Webb
 * @since 3.2.0
 */
public record NestedLocation(File file, String nestedEntryName) {

	private static final Map<String, NestedLocation> cache = new ConcurrentHashMap<>();

	public NestedLocation {
		if (file == null) {
			throw new IllegalArgumentException("'file' must not be null");
		}
		if (nestedEntryName == null || nestedEntryName.trim().isEmpty()) {
			throw new IllegalArgumentException("'nestedEntryName' must not be empty");
		}
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
		return parse(UrlDecoder.decode(url.getPath()));
	}

	static NestedLocation parse(String path) {
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("'path' must not be empty");
		}
		int index = path.lastIndexOf("/!");
		if (index == -1) {
			throw new IllegalArgumentException("'path' must contain '/!'");
		}
		return cache.computeIfAbsent(path, (l) -> create(index, l));
	}

	private static NestedLocation create(int index, String location) {
		String file = location.substring(0, index);
		String nestedEntryName = location.substring(index + 2);
		return new NestedLocation((!file.isEmpty()) ? new File(file) : null, nestedEntryName);
	}

	static void clearCache() {
		cache.clear();
	}

}
