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
import java.net.MalformedURLException;
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
 */
public abstract class ResourceUtils {

	/** Pseudo URL prefix for loading from the class path: "classpath:" */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/** Pseudo URL prefix for loading all resources from the class path: "classpath*:" */
	public static final String ALL_CLASSPATH_URL_PREFIX = "classpath*:";

	/** URL prefix for loading from the file system: "file:" */
	public static final String FILE_URL_PREFIX = "file:";

	/** Wildcard character in source path */
	public static final CharSequence WILDCARD = "*";

	public static List<String> getUrls(String path, ClassLoader classLoader) {

		if (classLoader == null) {
			classLoader = ClassUtils.getDefaultClassLoader();
		}

		path = StringUtils.cleanPath(path);
		if (path.contains(WILDCARD)) {
			if (path.contains(":")) {
				try {
					Resource[] resources = new PathMatchingResourcePatternResolver(
							classLoader).getResources(path);
					List<String> result = new ArrayList<String>();
					for (Resource resource : resources) {
						result.add(resource.getURL().toExternalForm());
					}
					return result;
				}
				catch (IOException e) {
					throw new IllegalArgumentException("Cannot resolve paths at [" + path
							+ "]", e);
				}
			}
			else {
				try {
					return getUrls(FILE_URL_PREFIX + path, classLoader);
				}
				catch (IllegalArgumentException e) {
					// ignore
				}
				return getUrls(ALL_CLASSPATH_URL_PREFIX + path, classLoader);
			}
		}

		if (path.contains(":")) {

			if (path.startsWith(CLASSPATH_URL_PREFIX)) {
				path = path.substring(CLASSPATH_URL_PREFIX.length());
			}
			else {
				return getFilePath(path);
			}

		}
		else {
			try {
				return getFilePath(path);
			}
			catch (IllegalArgumentException e) {
				// ignore
			}
		}

		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		List<String> result = new ArrayList<String>();
		if (classLoader != null) {
			Enumeration<URL> urls;
			try {
				urls = classLoader.getResources(path);
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

	private static List<String> getFilePath(String path) {
		FileSystemResource resource = new FileSystemResource(path);
		if (resource.exists()) {
			try {
				if (resource.getFile().isDirectory()) {
					Resource[] resources = new PathMatchingResourcePatternResolver()
							.getResources(resource.getURL() + "/**");
					List<String> result = new ArrayList<String>();
					for (Resource sub : resources) {
						if (!sub.getFile().isDirectory()) {
							result.add(sub.getURL().toExternalForm());
						}
					}
					return result;
				}
				return Collections.singletonList(resource.getURL().toExternalForm());
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Cannot create URL from path [" + path
						+ "]", e);
			}
		}
		try {
			UrlResource url = new UrlResource(path);
			if (url.exists()) {
				try {
					return Collections.singletonList(url.getURL().toExternalForm());
				}
				catch (IOException e) {
					throw new IllegalArgumentException("Cannot create URL from path ["
							+ path + "]", e);
				}
			}
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Cannot create URL from path [" + path
					+ "]", ex);
		}
		return Collections.emptyList();
	}
}
