/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;

/**
 * Loads the names of annotated classes, usually @{@link AutoConfiguration}.
 *
 * The names of the classes are stored in files named META-INF/spring-boot/{full qualified
 * name of the annotation}. Every line contains the full qualified class name of the
 * annotated class. Comments are supported using the # character.
 *
 * @author Moritz Halbritter
 * @see AutoConfiguration
 * @see SpringFactoriesLoader
 */
class AutoConfigurationLoader {

	private static final String LOCATION = "META-INF/spring-boot/";

	private static final String COMMENT_START = "#";

	/**
	 * Loads the names of annotated classes.
	 * @param annotation annotation to load
	 * @param classLoader class loader to use for loading
	 * @return list of names of annotated classes
	 */
	List<String> loadNames(Class<?> annotation, ClassLoader classLoader) {
		Assert.notNull(annotation, "'annotation' must not be null");
		ClassLoader classLoaderToUse = decideClassloader(classLoader);
		String location = LOCATION + annotation.getName();
		Enumeration<URL> urls = findUrlsInClasspath(classLoaderToUse, location);
		List<String> autoConfigurations = new ArrayList<>();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			autoConfigurations.addAll(readAutoConfigurations(url));
		}
		return autoConfigurations;
	}

	private ClassLoader decideClassloader(ClassLoader classLoader) {
		if (classLoader == null) {
			return AutoConfigurationLoader.class.getClassLoader();
		}
		return classLoader;
	}

	private Enumeration<URL> findUrlsInClasspath(ClassLoader classLoader, String location) {
		try {
			return classLoader.getResources(location);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to load autoconfigurations from location [" + location + "]",
					ex);
		}
	}

	private List<String> readAutoConfigurations(URL url) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new UrlResource(url).getInputStream(), StandardCharsets.UTF_8))) {
			List<String> autoConfigurations = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				line = stripComment(line);
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				autoConfigurations.add(line);
			}
			return autoConfigurations;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load autoconfigurations from location [" + url + "]", ex);
		}
	}

	private String stripComment(String line) {
		int commentStart = line.indexOf(COMMENT_START);
		if (commentStart == -1) {
			return line;
		}
		return line.substring(0, commentStart);
	}

}
