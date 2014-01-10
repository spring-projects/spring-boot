/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Utilities for manipulating resource paths and URLs.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public abstract class ResourceUtils {

	/**
	 * Pseudo URL prefix for loading from the class path: "classpath:"
	 */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/**
	 * Pseudo URL prefix for loading all resources from the class path: "classpath*:"
	 */
	public static final String ALL_CLASSPATH_URL_PREFIX = "classpath*:";

	/**
	 * URL prefix for loading from the file system: "file:"
	 */
	public static final String FILE_URL_PREFIX = "file:";

	/**
	 * Wildcard character in source path
	 */
	private static final CharSequence WILDCARD = "*";

	/**
	 * Return URLs from a given source path. Source paths can be simple file locations
	 * (/some/file.java) or wildcard patterns (/some/**). Additionally the prefixes
	 * "file:", "classpath:" and "classpath*:" can be used for specific path types.
	 * @param path the source path
	 * @param classLoader the class loader or {@code null} to use the default
	 * @return a list of URLs
	 */
	public static List<String> getUrls(String path, ClassLoader classLoader) {

		if (classLoader == null) {
			classLoader = ClassUtils.getDefaultClassLoader();
		}

		path = StringUtils.cleanPath(path);

		try {
			if (path.contains(WILDCARD)) {
				return getUrlsFromWildcardPath(path, classLoader);
			}

			if (path.contains(":")) {
				return getUrlsFromPrefixedPath(path, classLoader);
			}

			try {
				return getUrlsFromFile(path);
			}
			catch (IOException ex) {
				// ignore
			}

			return getUrlsFromResources(path, classLoader);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Cannot create URL from path [" + path
					+ "]", ex);

		}
	}

	private static List<String> getUrlsFromWildcardPath(String path,
			ClassLoader classLoader) throws IOException {
		if (path.contains(":")) {
			return getUrlsFromPrefixedWildcardPath(path, classLoader);
		}

		try {
			return getUrls(FILE_URL_PREFIX + path, classLoader);
		}
		catch (IllegalArgumentException ex) {
			// ignore
		}

		return getUrls(ALL_CLASSPATH_URL_PREFIX + path, classLoader);
	}

	private static List<String> getUrlsFromPrefixedWildcardPath(String path,
			ClassLoader classLoader) throws IOException {
		Resource[] resources = new PathMatchingResourcePatternResolver(classLoader)
				.getResources(path);
		List<String> result = new ArrayList<String>();
		for (Resource resource : resources) {
			result.add(resource.getURL().toExternalForm());
		}
		return result;
	}

	private static List<String> getUrlsFromPrefixedPath(String path,
			ClassLoader classLoader) throws IOException {
		if (path.startsWith(CLASSPATH_URL_PREFIX)) {
			return getUrlsFromResources(path.substring(CLASSPATH_URL_PREFIX.length()),
					classLoader);
		}
		return getUrlsFromFile(path);
	}

	private static List<String> getUrlsFromFile(String path) throws IOException {
		Resource resource = new FileSystemResource(path);
		if (resource.exists()) {
			if (resource.getFile().isDirectory()) {
				return getChildFiles(resource);
			}
			return Collections.singletonList(resource.getURL().toExternalForm());
		}

		resource = new UrlResource(path);
		if (resource.exists()) {
			return Collections.singletonList(resource.getURL().toExternalForm());
		}

		return Collections.emptyList();
	}

	private static List<String> getChildFiles(Resource resource) throws IOException {
		Resource[] children = new PathMatchingResourcePatternResolver()
				.getResources(resource.getURL() + "/**");
		List<String> childFiles = new ArrayList<String>();
		for (Resource child : children) {
			if (!child.getFile().isDirectory()) {
				childFiles.add(child.getURL().toExternalForm());
			}
		}
		return childFiles;
	}

	private static List<String> getUrlsFromResources(String path, ClassLoader classLoader) {
		path = stripLeadingSlashes(path);
		List<String> result = new ArrayList<String>();
		if (classLoader != null) {
			try {
				Enumeration<URL> urls = classLoader.getResources(path);
				while (urls.hasMoreElements()) {
					URL url = urls.nextElement();
					result.add(url.toExternalForm());
				}
			}
			catch (IOException e) {
				// Ignore
			}
		}
		return result;
	}

	private static String stripLeadingSlashes(String path) {
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		return path;
	}

}
