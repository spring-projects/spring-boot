/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter.annotation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextAnnotationUtils.AnnotationDescriptor;

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
		AnnotationDescriptor<TypeExcludeFilters> descriptor = TestContextAnnotationUtils
			.findAnnotationDescriptor(testClass, TypeExcludeFilters.class);
		if (descriptor == null) {
			return null;
		}
		Class<?>[] filterClasses = descriptor.getAnnotation().value();
		return createContextCustomizer(descriptor.getRootDeclaringClass(), filterClasses);
	}

	@SuppressWarnings("unchecked")
	private ContextCustomizer createContextCustomizer(Class<?> testClass, Class<?>[] filterClasses) {
		return new TypeExcludeFiltersContextCustomizer(testClass,
				new LinkedHashSet<>(Arrays.asList((Class<? extends TypeExcludeFilter>[]) filterClasses)));
	}

}
