/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Internal utility class to scan for a {@link SpringBootConfiguration} class.
 *
 * @author Phillip Webb
 */
final class SpringBootConfigurationFinder {

	private static final Map<String, Class<?>> cache = Collections
			.synchronizedMap(new Cache(40));

	private final ClassPathScanningCandidateComponentProvider scanner;

	SpringBootConfigurationFinder() {
		this.scanner = new ClassPathScanningCandidateComponentProvider(false);
		this.scanner.addIncludeFilter(
				new AnnotationTypeFilter(SpringBootConfiguration.class));
		this.scanner.setResourcePattern("*.class");
	}

	public Class<?> findFromClass(Class<?> source) {
		Assert.notNull(source, "Source must not be null");
		return findFromPackage(ClassUtils.getPackageName(source));
	}

	public Class<?> findFromPackage(String source) {
		Assert.notNull(source, "Source must not be null");
		Class<?> configuration = cache.get(source);
		if (configuration == null) {
			configuration = scanPackage(source);
			cache.put(source, configuration);
		}
		return configuration;
	}

	private Class<?> scanPackage(String source) {
		while (source.length() > 0) {
			Set<BeanDefinition> components = this.scanner.findCandidateComponents(source);
			if (!components.isEmpty()) {
				Assert.state(components.size() == 1,
						"Found multiple @SpringBootConfiguration annotated classes "
								+ components);
				return ClassUtils.resolveClassName(
						components.iterator().next().getBeanClassName(), null);
			}
			source = getParentPackage(source);
		}
		return null;
	}

	private String getParentPackage(String sourcePackage) {
		int lastDot = sourcePackage.lastIndexOf(".");
		return (lastDot != -1) ? sourcePackage.substring(0, lastDot) : "";
	}

	/**
	 * Cache implementation based on {@link LinkedHashMap}.
	 */
	private static class Cache extends LinkedHashMap<String, Class<?>> {

		private final int maxSize;

		Cache(int maxSize) {
			super(16, 0.75f, true);
			this.maxSize = maxSize;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Class<?>> eldest) {
			return size() > this.maxSize;
		}

	}

}
