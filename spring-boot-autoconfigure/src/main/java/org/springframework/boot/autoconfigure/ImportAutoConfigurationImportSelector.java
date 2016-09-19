/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.Set;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * Variant of {@link EnableAutoConfigurationImportSelector} for
 * {@link ImportAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ImportAutoConfigurationImportSelector
		extends EnableAutoConfigurationImportSelector {

	private static final Set<String> ANNOTATION_NAMES;

	static {
		Set<String> names = new LinkedHashSet<String>();
		names.add(ImportAutoConfiguration.class.getName());
		names.add("org.springframework.boot.autoconfigure.test.ImportAutoConfiguration");
		ANNOTATION_NAMES = Collections.unmodifiableSet(names);
	}

	@Override
	protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
		return null;
	}

	@Override
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		try {
			return getCandidateConfigurations(
					ClassUtils.forName(metadata.getClassName(), null));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private List<String> getCandidateConfigurations(Class<?> source) {
		Set<String> candidates = new LinkedHashSet<String>();
		collectCandidateConfigurations(source, candidates, new HashSet<Class<?>>());
		return new ArrayList<String>(candidates);
	}

	private void collectCandidateConfigurations(Class<?> source, Set<String> candidates,
			Set<Class<?>> seen) {
		if (source != null && seen.add(source)) {
			for (Annotation annotation : source.getDeclaredAnnotations()) {
				if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
					collectCandidateConfigurations(source, annotation, candidates, seen);
				}
			}
			collectCandidateConfigurations(source.getSuperclass(), candidates, seen);
		}
	}

	private void collectCandidateConfigurations(Class<?> source, Annotation annotation,
			Set<String> candidates, Set<Class<?>> seen) {
		if (ANNOTATION_NAMES.contains(annotation.annotationType().getName())) {
			candidates.addAll(getConfigurationsForAnnotation(source, annotation));
		}
		collectCandidateConfigurations(annotation.annotationType(), candidates, seen);
	}

	private Collection<String> getConfigurationsForAnnotation(Class<?> source,
			Annotation annotation) {
		String[] value = (String[]) AnnotationUtils
				.getAnnotationAttributes(annotation, true).get("value");
		if (value.length > 0) {
			return Arrays.asList(value);
		}
		return SpringFactoriesLoader.loadFactoryNames(source,
				getClass().getClassLoader());
	}

	@Override
	protected Set<String> getExclusions(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		return Collections.emptySet();
	}

	@Override
	public int getOrder() {
		return super.getOrder() - 1;
	}

}
