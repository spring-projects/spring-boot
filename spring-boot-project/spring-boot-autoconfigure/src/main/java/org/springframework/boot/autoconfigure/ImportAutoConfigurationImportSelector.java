/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.Set;

import org.springframework.boot.context.annotation.DeterminableImports;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Variant of {@link AutoConfigurationImportSelector} for {@link ImportAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ImportAutoConfigurationImportSelector extends AutoConfigurationImportSelector
		implements DeterminableImports {

	private static final Set<String> ANNOTATION_NAMES;

	static {
		Set<String> names = new LinkedHashSet<>();
		names.add(ImportAutoConfiguration.class.getName());
		names.add("org.springframework.boot.autoconfigure.test.ImportAutoConfiguration");
		ANNOTATION_NAMES = Collections.unmodifiableSet(names);
	}

	@Override
	public Set<Object> determineImports(AnnotationMetadata metadata) {
		Set<String> result = new LinkedHashSet<>();
		result.addAll(getCandidateConfigurations(metadata, null));
		result.removeAll(getExclusions(metadata, null));
		return Collections.unmodifiableSet(result);
	}

	@Override
	protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
		return null;
	}

	@Override
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		List<String> candidates = new ArrayList<>();
		Map<Class<?>, List<Annotation>> annotations = getAnnotations(metadata);
		annotations.forEach((source, sourceAnnotations) -> collectCandidateConfigurations(
				source, sourceAnnotations, candidates));
		return candidates;
	}

	private void collectCandidateConfigurations(Class<?> source,
			List<Annotation> annotations, List<String> candidates) {
		for (Annotation annotation : annotations) {
			candidates.addAll(getConfigurationsForAnnotation(source, annotation));
		}
	}

	private Collection<String> getConfigurationsForAnnotation(Class<?> source,
			Annotation annotation) {
		String[] classes = (String[]) AnnotationUtils
				.getAnnotationAttributes(annotation, true).get("classes");
		if (classes.length > 0) {
			return Arrays.asList(classes);
		}
		return loadFactoryNames(source);
	}

	protected Collection<String> loadFactoryNames(Class<?> source) {
		return SpringFactoriesLoader.loadFactoryNames(source,
				getClass().getClassLoader());
	}

	@Override
	protected Set<String> getExclusions(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		Set<String> exclusions = new LinkedHashSet<>();
		Class<?> source = ClassUtils.resolveClassName(metadata.getClassName(), null);
		for (String annotationName : ANNOTATION_NAMES) {
			AnnotationAttributes merged = AnnotatedElementUtils
					.getMergedAnnotationAttributes(source, annotationName);
			Class<?>[] exclude = (merged != null) ? merged.getClassArray("exclude")
					: null;
			if (exclude != null) {
				for (Class<?> excludeClass : exclude) {
					exclusions.add(excludeClass.getName());
				}
			}
		}
		for (List<Annotation> annotations : getAnnotations(metadata).values()) {
			for (Annotation annotation : annotations) {
				String[] exclude = (String[]) AnnotationUtils
						.getAnnotationAttributes(annotation, true).get("exclude");
				if (!ObjectUtils.isEmpty(exclude)) {
					exclusions.addAll(Arrays.asList(exclude));
				}
			}
		}
		return exclusions;
	}

	protected final Map<Class<?>, List<Annotation>> getAnnotations(
			AnnotationMetadata metadata) {
		MultiValueMap<Class<?>, Annotation> annotations = new LinkedMultiValueMap<>();
		Class<?> source = ClassUtils.resolveClassName(metadata.getClassName(), null);
		collectAnnotations(source, annotations, new HashSet<>());
		return Collections.unmodifiableMap(annotations);
	}

	private void collectAnnotations(Class<?> source,
			MultiValueMap<Class<?>, Annotation> annotations, HashSet<Class<?>> seen) {
		if (source != null && seen.add(source)) {
			for (Annotation annotation : source.getDeclaredAnnotations()) {
				if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
					if (ANNOTATION_NAMES
							.contains(annotation.annotationType().getName())) {
						annotations.add(source, annotation);
					}
					collectAnnotations(annotation.annotationType(), annotations, seen);
				}
			}
			collectAnnotations(source.getSuperclass(), annotations, seen);
		}
	}

	@Override
	public int getOrder() {
		return super.getOrder() - 1;
	}

	@Override
	protected void handleInvalidExcludes(List<String> invalidExcludes) {
		// Ignore for test
	}

}
