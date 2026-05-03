/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.context.filter.annotation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Contains types to be included by a {@link TypeExcludeFilter}.
 *
 * The {@link #load(Class, ClassLoader)} method can be used to discover the includes.
 *
 * @author Moritz Halbritter
 * @author Scott Frederick
 * @author Andy Wilkinson
 */
final class TypeIncludes implements Iterable<Class<?>> {

	private static final String LOCATION = "META-INF/spring/%s.includes";

	private static final String COMMENT_START = "#";

	private static final Log logger = LogFactory.getLog(TypeIncludes.class);

	private final Set<Class<?>> includes;

	private TypeIncludes(Set<Class<?>> includes) {
		Assert.notNull(includes, "'includes' must not be null");
		this.includes = Collections.unmodifiableSet(includes);
	}

	@Override
	public Iterator<Class<?>> iterator() {
		return this.includes.iterator();
	}

	Set<Class<?>> getIncludes() {
		return this.includes;
	}

	/**
	 * Loads the includes from the classpath. The names of the includes are stored in
	 * files named {@code META-INF/spring/fully-qualified-annotation-name.includes} on the
	 * classpath. Every line contains the fully qualified name of the included class.
	 * Comments are supported using the # character.
	 * @param annotation annotation to load
	 * @param classLoader class loader to use for loading
	 * @return list of names of included classes
	 */
	static TypeIncludes load(Class<?> annotation, @Nullable ClassLoader classLoader) {
		Assert.notNull(annotation, "'annotation' must not be null");
		ClassLoader classLoaderToUse = decideClassloader(classLoader);
		String location = String.format(LOCATION, annotation.getName());
		Enumeration<URL> urls = findUrlsInClasspath(classLoaderToUse, location);
		Set<Class<?>> includes = new HashSet<>();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			includes.addAll(loadIncludes(url, classLoader));
		}
		return new TypeIncludes(includes);
	}

	private static ClassLoader decideClassloader(@Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			return TypeIncludes.class.getClassLoader();
		}
		return classLoader;
	}

	private static Enumeration<URL> findUrlsInClasspath(ClassLoader classLoader, String location) {
		try {
			return classLoader.getResources(location);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to load includes from location [" + location + "]", ex);
		}
	}

	private static Set<Class<?>> loadIncludes(URL url, @Nullable ClassLoader classLoader) {
		Set<String> includeNames = readIncludes(url);
		Set<Class<?>> includes = new HashSet<>(includeNames.size());
		for (String includeName : includeNames) {
			try {
				includes.add(ClassUtils.forName(includeName, classLoader));
			}
			catch (Exception ex) {
				logger.debug(LogMessage.format("Include '%s' declared in '%s' could not be loaded and has been ignored",
						includeName, url), ex);
			}
		}
		return includes;
	}

	private static Set<String> readIncludes(URL url) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new UrlResource(url).getInputStream(), StandardCharsets.UTF_8))) {
			Set<String> includes = new HashSet<>();
			String line;
			while ((line = reader.readLine()) != null) {
				line = stripComment(line);
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				includes.add(line);
			}
			return includes;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load includes from location [" + url + "]", ex);
		}
	}

	private static String stripComment(String line) {
		int commentStart = line.indexOf(COMMENT_START);
		if (commentStart == -1) {
			return line;
		}
		return line.substring(0, commentStart);
	}

}
