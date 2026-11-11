/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter.annotation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} to support
 * {@link TypeExcludeFilters @TypeExcludeFilters}.
 *
 * @author Phillip Webb
 * @see TypeExcludeFiltersContextCustomizer
 */
class TypeExcludeFiltersContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configurationAttributes) {
		if (AotDetector.useGeneratedArtifacts()) {
			return null;
		}
		Set<Class<? extends TypeExcludeFilter>> filterClasses = MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
			.withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
			.from(testClass)
			.stream(TypeExcludeFilters.class)
			.map(MergedAnnotation::synthesize)
			.flatMap((annotation) -> Arrays.stream(annotation.value()))
			.collect(Collectors.toCollection(LinkedHashSet::new));
		if (filterClasses.isEmpty()) {
			return null;
		}
		return new TypeExcludeFiltersContextCustomizer(testClass, filterClasses);
	}

}
