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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility class to find a class annotated with a particular annotation in a hierarchy.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public final class AnnotatedClassFinder {

	private static final Map<String, Class<?>> cache = Collections
			.synchronizedMap(new Cache(40));

	private final Class<? extends Annotation> annotationType;

	private final ClassPathScanningCandidateComponentProvider scanner;

	/**
	 * Create a new instance with the {@code annotationType} to find.
	 * @param annotationType the annotation to find
	 */
	public AnnotatedClassFinder(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "AnnotationType must not be null");
		this.annotationType = annotationType;
		this.scanner = new ClassPathScanningCandidateComponentProvider(false);
		this.scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
		this.scanner.setResourcePattern("*.class");
	}

	/**
	 * Find the first {@link Class} that is annotated with the target annotation, starting
	 * from the package defined by the given {@code source} up to the root.
	 * @param source the source class to use to initiate the search
	 * @return the first {@link Class} annotated with the target annotation within the
	 * hierarchy defined by the given {@code source} or {@code null} if none is found.
	 */
	public Class<?> findFromClass(Class<?> source) {
		Assert.notNull(source, "Source must not be null");
		return findFromPackage(ClassUtils.getPackageName(source));
	}

	/**
	 * Find the first {@link Class} that is annotated with the target annotation, starting
	 * from the package defined by the given {@code source} up to the root.
	 * @param source the source package to use to initiate the search
	 * @return the first {@link Class} annotated with the target annotation within the
	 * hierarchy defined by the given {@code source} or {@code null} if none is found.
	 */
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
		while (!source.isEmpty()) {
			Set<BeanDefinition> components = this.scanner.findCandidateComponents(source);
			if (!components.isEmpty()) {
				Assert.state(components.size() == 1,
						() -> "Found multiple @" + this.annotationType.getSimpleName()
								+ " annotated classes " + components);
				return ClassUtils.resolveClassName(
						components.iterator().next().getBeanClassName(), null);
			}
			source = getParentPackage(source);
		}
		return null;
	}

	private String getParentPackage(String sourcePackage) {
		int lastDot = sourcePackage.lastIndexOf('.');
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
