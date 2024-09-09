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

package org.springframework.boot.autoconfigure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.core.io.UrlResource;
import org.springframework.util.Assert;

/**
 * Contains auto-configuration replacements used to handle deprecated or moved
 * auto-configurations which may still be referenced by
 * {@link AutoConfigureBefore @AutoConfigureBefore},
 * {@link AutoConfigureAfter @AutoConfigureAfter} or exclusions.
 *
 * @author Phillip Webb
 */
final class AutoConfigurationReplacements {

	private static final String LOCATION = "META-INF/spring/%s.replacements";

	private final Map<String, String> replacements;

	private AutoConfigurationReplacements(Map<String, String> replacements) {
		this.replacements = Map.copyOf(replacements);
	}

	Set<String> replaceAll(Set<String> classNames) {
		Set<String> replaced = new LinkedHashSet<>(classNames.size());
		for (String className : classNames) {
			replaced.add(replace(className));
		}
		return replaced;
	}

	String replace(String className) {
		return this.replacements.getOrDefault(className, className);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.replacements.equals(((AutoConfigurationReplacements) obj).replacements);
	}

	@Override
	public int hashCode() {
		return this.replacements.hashCode();
	}

	/**
	 * Loads the relocations from the classpath. Relactions are stored in files named
	 * {@code META-INF/spring/full-qualified-annotation-name.replacements} on the
	 * classpath. The file is loaded using {@link Properties#load(java.io.InputStream)}
	 * with each entry containing an auto-configuration class name as the key and the
	 * replacement class name as the value.
	 * @param annotation annotation to load
	 * @param classLoader class loader to use for loading
	 * @return list of names of annotated classes
	 */
	static AutoConfigurationReplacements load(Class<?> annotation, ClassLoader classLoader) {
		Assert.notNull(annotation, "'annotation' must not be null");
		ClassLoader classLoaderToUse = decideClassloader(classLoader);
		String location = String.format(LOCATION, annotation.getName());
		Enumeration<URL> urls = findUrlsInClasspath(classLoaderToUse, location);
		Map<String, String> replacements = new HashMap<>();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			replacements.putAll(readReplacements(url));
		}
		return new AutoConfigurationReplacements(replacements);
	}

	private static ClassLoader decideClassloader(ClassLoader classLoader) {
		if (classLoader == null) {
			return ImportCandidates.class.getClassLoader();
		}
		return classLoader;
	}

	private static Enumeration<URL> findUrlsInClasspath(ClassLoader classLoader, String location) {
		try {
			return classLoader.getResources(location);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to load configurations from location [" + location + "]", ex);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map<String, String> readReplacements(URL url) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new UrlResource(url).getInputStream(), StandardCharsets.UTF_8))) {
			Properties properties = new Properties();
			properties.load(reader);
			return (Map) properties;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load replacements from location [" + url + "]", ex);
		}
	}

}
