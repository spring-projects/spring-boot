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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;

/**
 * Utility class with factory methods that can be used to create JAR URLs.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public final class JarUrl {

	private JarUrl() {
	}

	/**
	 * Create a new jar URL.
	 * @param file the jar file
	 * @return a jar file URL
	 */
	public static URL create(File file) {
		return create(file, (String) null);
	}

	/**
	 * Create a new jar URL.
	 * @param file the jar file
	 * @param nestedEntry the nested entry or {@code null}
	 * @return a jar file URL
	 */
	public static URL create(File file, JarEntry nestedEntry) {
		return create(file, (nestedEntry != null) ? nestedEntry.getName() : null);
	}

	/**
	 * Create a new jar URL.
	 * @param file the jar file
	 * @param nestedEntryName the nested entry name or {@code null}
	 * @return a jar file URL
	 */
	public static URL create(File file, String nestedEntryName) {
		return create(file, nestedEntryName, null);
	}

	/**
	 * Create a new jar URL.
	 * @param file the jar file
	 * @param nestedEntryName the nested entry name or {@code null}
	 * @param path the path within the jar or nested jar
	 * @return a jar file URL
	 */
	public static URL create(File file, String nestedEntryName, String path) {
		try {
			path = (path != null) ? path : "";
			return new URL(null, "jar:" + getJarReference(file, nestedEntryName) + "!/" + path, Handler.INSTANCE);
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Unable to create JarFileArchive URL", ex);
		}
	}

	private static String getJarReference(File file, String nestedEntryName) {
		String jarFilePath = file.toURI().getPath();
		return (nestedEntryName != null) ? "nested:" + jarFilePath + "/!" + nestedEntryName : "file:" + jarFilePath;
	}

}
