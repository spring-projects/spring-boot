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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.context.annotation.DeterminableImports;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Variant of {@link AutoConfigurationImportSelector} for
 * {@link ImportAutoConfiguration @ImportAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Scott Frederick
 */
class ImportAutoConfigurationImportSelector extends AutoConfigurationImportSelector implements DeterminableImports {

	private static final String OPTIONAL_PREFIX = "optional:";

	private static final Set<String> ANNOTATION_NAMES;

	static {
		Set<String> names = new LinkedHashSet<>();
		names.add(ImportAutoConfiguration.class.getName());
		names.add("org.springframework.boot.autoconfigure.test.ImportAutoConfiguration");
		ANNOTATION_NAMES = Collections.unmodifiableSet(names);
	}

	/**
	 * Determines the imports for the given annotation metadata.
	 * @param metadata the annotation metadata
	 * @return the set of imports
	 */
	@Override
	public Set<Object> determineImports(AnnotationMetadata metadata) {
		List<String> candidateConfigurations = getCandidateConfigurations(metadata, null);
		Set<String> result = new LinkedHashSet<>(candidateConfigurations);
		result.removeAll(getExclusions(metadata, null));
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Retrieves the attributes of the specified annotation metadata.
	 * @param metadata the annotation metadata to retrieve attributes from
	 * @return the attributes of the annotation metadata, or null if no attributes are
	 * found
	 */
	@Override
	protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
		return null;
	}

	/**
	 * Retrieves the candidate configurations based on the provided metadata and
	 * attributes.
	 * @param metadata the metadata of the import
	 * @param attributes the attributes of the import
	 * @return the list of candidate configurations
	 */
	@Override
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
		List<String> candidates = new ArrayList<>();
		Map<Class<?>, List<Annotation>> annotations = getAnnotations(metadata);
		annotations.forEach(
				(source, sourceAnnotations) -> collectCandidateConfigurations(source, sourceAnnotations, candidates));
		return candidates;
	}

	/**
	 * Collects candidate configurations for the given source class and list of
	 * annotations.
	 * @param source the source class
	 * @param annotations the list of annotations
	 * @param candidates the list to store the candidate configurations
	 */
	private void collectCandidateConfigurations(Class<?> source, List<Annotation> annotations,
			List<String> candidates) {
		for (Annotation annotation : annotations) {
			candidates.addAll(getConfigurationsForAnnotation(source, annotation));
		}
	}

	/**
	 * Retrieves the configurations for a given annotation and source class.
	 * @param source the source class
	 * @param annotation the annotation to retrieve configurations for
	 * @return a collection of configuration classes
	 */
	private Collection<String> getConfigurationsForAnnotation(Class<?> source, Annotation annotation) {
		String[] classes = (String[]) AnnotationUtils.getAnnotationAttributes(annotation, true).get("classes");
		if (classes.length > 0) {
			return Arrays.asList(classes);
		}
		return loadFactoryNames(source).stream().map(this::mapFactoryName).filter(Objects::nonNull).toList();
	}

	/**
	 * Maps the factory name by removing the optional prefix if present.
	 * @param name the factory name to be mapped
	 * @return the mapped factory name without the optional prefix, or null if the mapped
	 * name is not present
	 */
	private String mapFactoryName(String name) {
		if (!name.startsWith(OPTIONAL_PREFIX)) {
			return name;
		}
		name = name.substring(OPTIONAL_PREFIX.length());
		return (!present(name)) ? null : name;
	}

	/**
	 * Checks if a given class is present in the classpath.
	 * @param className the name of the class to check
	 * @return true if the class is present, false otherwise
	 */
	private boolean present(String className) {
		String resourcePath = ClassUtils.convertClassNameToResourcePath(className) + ".class";
		return new ClassPathResource(resourcePath).exists();
	}

	/**
	 * Loads the factory names from the given source class.
	 * @param source the source class from which to load the factory names
	 * @return a collection of factory names
	 */
	protected Collection<String> loadFactoryNames(Class<?> source) {
		return ImportCandidates.load(source, getBeanClassLoader()).getCandidates();
	}

	/**
	 * Retrieves the set of exclusions for the given metadata and attributes.
	 * @param metadata the annotation metadata
	 * @param attributes the annotation attributes
	 * @return the set of exclusions
	 */
	@Override
	protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
		Set<String> exclusions = new LinkedHashSet<>();
		Class<?> source = ClassUtils.resolveClassName(metadata.getClassName(), getBeanClassLoader());
		for (String annotationName : ANNOTATION_NAMES) {
			AnnotationAttributes merged = AnnotatedElementUtils.getMergedAnnotationAttributes(source, annotationName);
			Class<?>[] exclude = (merged != null) ? merged.getClassArray("exclude") : null;
			if (exclude != null) {
				for (Class<?> excludeClass : exclude) {
					exclusions.add(excludeClass.getName());
				}
			}
		}
		for (List<Annotation> annotations : getAnnotations(metadata).values()) {
			for (Annotation annotation : annotations) {
				String[] exclude = (String[]) AnnotationUtils.getAnnotationAttributes(annotation, true).get("exclude");
				if (!ObjectUtils.isEmpty(exclude)) {
					exclusions.addAll(Arrays.asList(exclude));
				}
			}
		}
		exclusions.addAll(getExcludeAutoConfigurationsProperty());
		return exclusions;
	}

	/**
	 * Retrieves the annotations present on the given {@link AnnotationMetadata}.
	 * @param metadata the {@link AnnotationMetadata} to retrieve annotations from
	 * @return an unmodifiable {@link Map} containing the annotations, grouped by their
	 * corresponding classes
	 */
	protected final Map<Class<?>, List<Annotation>> getAnnotations(AnnotationMetadata metadata) {
		MultiValueMap<Class<?>, Annotation> annotations = new LinkedMultiValueMap<>();
		Class<?> source = ClassUtils.resolveClassName(metadata.getClassName(), getBeanClassLoader());
		collectAnnotations(source, annotations, new HashSet<>());
		return Collections.unmodifiableMap(annotations);
	}

	/**
	 * Recursively collects annotations from the given source class and its superclasses.
	 * @param source the source class to collect annotations from
	 * @param annotations the map to store collected annotations
	 * @param seen the set to keep track of already visited classes to avoid infinite
	 * recursion
	 */
	private void collectAnnotations(Class<?> source, MultiValueMap<Class<?>, Annotation> annotations,
			HashSet<Class<?>> seen) {
		if (source != null && seen.add(source)) {
			for (Annotation annotation : source.getDeclaredAnnotations()) {
				if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
					if (ANNOTATION_NAMES.contains(annotation.annotationType().getName())) {
						annotations.add(source, annotation);
					}
					collectAnnotations(annotation.annotationType(), annotations, seen);
				}
			}
			collectAnnotations(source.getSuperclass(), annotations, seen);
		}
	}

	/**
	 * Returns the order of this ImportAutoConfigurationImportSelector. The order is
	 * determined by subtracting 1 from the order of the superclass.
	 * @return the order of this ImportAutoConfigurationImportSelector
	 */
	@Override
	public int getOrder() {
		return super.getOrder() - 1;
	}

	/**
	 * Handles invalid excludes.
	 * @param invalidExcludes the list of invalid excludes
	 */
	@Override
	protected void handleInvalidExcludes(List<String> invalidExcludes) {
		// Ignore for test
	}

}
