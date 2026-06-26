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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TypeExcludeFiltersContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
class TypeExcludeFiltersContextCustomizerFactoryTests {

	private final TypeExcludeFiltersContextCustomizerFactory factory = new TypeExcludeFiltersContextCustomizerFactory();

	private final MergedContextConfiguration mergedContextConfiguration = mock(MergedContextConfiguration.class);

	private final ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	void getContextCustomizerWhenHasNoAnnotationShouldReturnNull() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(NoAnnotation.class,
				Collections.emptyList());
		assertThat(customizer).isNull();
	}

	@Test
	void getContextCustomizerWhenHasAnnotationShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(WithExcludeFilters.class,
				Collections.emptyList());
		assertThat(customizer).isNotNull();
	}

	@Test
	void getContextCustomizerWhenEnclosingClassHasAnnotationShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory
			.createContextCustomizer(EnclosingClass.WithEnclosingClassExcludeFilters.class, Collections.emptyList());
		assertThat(customizer).isNotNull();
	}

	@Test
	void getContextCustomizerWhenEnclosingClassHasAnnotationButNestedConfigurationIsOverrideShouldReturnNull() {
		ContextCustomizer customizer = this.factory
			.createContextCustomizer(EnclosingWithOverride.InnerWithOverride.class, Collections.emptyList());
		assertThat(customizer).isNull();
	}

	@Test
	void hashCodeAndEquals() {
		ContextCustomizer customizer1 = this.factory.createContextCustomizer(WithExcludeFilters.class,
				Collections.emptyList());
		ContextCustomizer customizer2 = this.factory.createContextCustomizer(WithSameExcludeFilters.class,
				Collections.emptyList());
		ContextCustomizer customizer3 = this.factory.createContextCustomizer(WithDifferentExcludeFilters.class,
				Collections.emptyList());
		assertThat(customizer1).hasSameHashCodeAs(customizer2);
		assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2).isNotEqualTo(customizer3);
	}

	@Test
	void getContextCustomizerShouldAddExcludeFilters() throws Exception {
		typeExcludeFiltersFor(WithExcludeFilters.class).doesNotMatch(NoAnnotation.class)
			.matches(SimpleExclude.class, TestClassAwareExclude.class);
	}

	@Test
	void getContextCustomizerWhenEnclosingClassHasAnnotationShouldAddExcludeFilters() throws Exception {
		typeExcludeFiltersFor(EnclosingClass.WithEnclosingClassExcludeFilters.class).matches(SimpleExclude.class,
				TestClassAwareExclude.class);
	}

	@Test
	void getContextCustomizerWhenHasDuplicateSliceExcludeFilterShouldInstantiateItOnce() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(WithDuplicateSliceExclude.class,
				Collections.emptyList());
		assertThat(customizer).extracting("filters", InstanceOfAssertFactories.collection(TypeExcludeFilter.class))
			.hasSize(1);
	}

	@Test
	void getContextCustomizerWhenEnclosingClassHasAnnotationsTypeExcludeFilters() throws Exception {
		typeExcludeFiltersFor(WithMultipleExcludeFilterAnnotations.WithEnclosingClassExcludeFilters.class)
			.matches(FirstSliceExclude.class, SecondSliceExclude.class);

	}

	@Test
	void getContextCustomizerWhenSuperclassHasAnnotationShouldIncludeTypeExcludeFilters() throws Exception {
		typeExcludeFiltersFor(WithMixedInheritance.class).matches(TestClassAwareExclude.class, FirstSliceExclude.class,
				SecondSliceExclude.class, ThirdSliceExclude.class);
	}

	@Test
	void getContextCustomizerWhenHasNestedComposedAnnotationShouldIncludeTypeExcludeFilters() throws Exception {
		typeExcludeFiltersFor(WithComposedAnnotation.class).matches(FirstSliceExclude.class);
	}

	@Test
	void getContextCustomizerWhenDeeplyNestedShouldIncludeAllEnclosingExcludeFilters() throws Exception {
		typeExcludeFiltersFor(GrandparentEnclosing.ParentEnclosing.DeepInnerClass.class)
			.matches(FirstSliceExclude.class, SecondSliceExclude.class, ThirdSliceExclude.class);
	}

	private TypeExcludeFilterAssert typeExcludeFiltersFor(Class<?> testClass) {
		ContextCustomizer customizer = this.factory.createContextCustomizer(testClass, Collections.emptyList());
		assertThat(customizer).isNotNull();
		customizer.customizeContext(this.context, this.mergedContextConfiguration);
		this.context.refresh();
		return new TypeExcludeFilterAssert(this.context.getBean(TypeExcludeFilter.class));
	}

	static class NoAnnotation {

	}

	@TypeExcludeFilters({ SimpleExclude.class, TestClassAwareExclude.class })
	static class WithExcludeFilters {

	}

	@TypeExcludeFilters({ SimpleExclude.class, TestClassAwareExclude.class })
	static class EnclosingClass {

		class WithEnclosingClassExcludeFilters {

		}

	}

	@TypeExcludeFilters({ TestClassAwareExclude.class, SimpleExclude.class })
	static class WithSameExcludeFilters {

	}

	@TypeExcludeFilters(SimpleExclude.class)
	static class WithDifferentExcludeFilters {

	}

	static class SimpleExclude extends TypeExcludeFilter {

		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
			return metadataReader.getClassMetadata().getClassName().equals(getClass().getName());
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			return obj != null && obj.getClass() == getClass();
		}

		@Override
		public int hashCode() {
			return SimpleExclude.class.hashCode();
		}

	}

	static class TestClassAwareExclude extends SimpleExclude {

		TestClassAwareExclude(Class<?> testClass) {
			assertThat(testClass).isNotNull();
		}

	}

	@FirstTestSlice
	@TypeExcludeFilters(SecondSliceExclude.class)
	static class WithMultipleExcludeFilterAnnotations {

		class WithEnclosingClassExcludeFilters {

		}

	}

	@TypeExcludeFilters(TestClassAwareExclude.class)
	static class WithMixedInheritance extends WithFirstTestSliceExclude {

	}

	@FirstTestSlice
	static class WithFirstTestSliceExclude implements WithSecondTestSliceExclude {

	}

	@SecondTestSlice
	interface WithSecondTestSliceExclude extends WithThirdTestSliceExclude {

	}

	@TypeExcludeFilters(ThirdSliceExclude.class)
	interface WithThirdTestSliceExclude {

	}

	@ComposedFirstTestSlice
	static class WithComposedAnnotation {

	}

	@FirstTestSlice
	@TypeExcludeFilters(FirstSliceExclude.class)
	static class WithDuplicateSliceExclude {

	}

	@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@TypeExcludeFilters(FirstSliceExclude.class)
	@interface FirstTestSlice {

	}

	@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@TypeExcludeFilters(SecondSliceExclude.class)
	@interface SecondTestSlice {

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@FirstTestSlice
	@interface ComposedFirstTestSlice {

	}

	@TypeExcludeFilters(FirstSliceExclude.class)
	static class EnclosingWithOverride {

		@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
		class InnerWithOverride {

		}

	}

	@TypeExcludeFilters(FirstSliceExclude.class)
	static class GrandparentEnclosing {

		@TypeExcludeFilters(SecondSliceExclude.class)
		class ParentEnclosing {

			@TypeExcludeFilters(ThirdSliceExclude.class)
			class DeepInnerClass {

			}

		}

	}

	static class FirstSliceExclude extends TestClassAwareExclude {

		FirstSliceExclude(Class<?> testClass) {
			super(testClass);
		}

	}

	static class SecondSliceExclude extends TestClassAwareExclude {

		SecondSliceExclude(Class<?> testClass) {
			super(testClass);
		}

	}

	static class ThirdSliceExclude extends TestClassAwareExclude {

		ThirdSliceExclude(Class<?> testClass) {
			super(testClass);
		}

	}

	private static final class TypeExcludeFilterAssert {

		private final TypeExcludeFilter filter;

		private final MetadataReaderFactory metadataReaderFactory = MetadataReaderFactory
			.create(new DefaultResourceLoader());

		private TypeExcludeFilterAssert(TypeExcludeFilter filter) {
			this.filter = filter;
		}

		TypeExcludeFilterAssert matches(Class<?>... types) throws Exception {
			for (Class<?> type : types) {
				assertThat(matches(type)).as("Filter should match %s", type.getName()).isTrue();
			}
			return this;
		}

		TypeExcludeFilterAssert doesNotMatch(Class<?>... types) throws Exception {
			for (Class<?> type : types) {
				assertThat(matches(type)).as("Filter should not match %s", type.getName()).isFalse();
			}
			return this;
		}

		private boolean matches(Class<?> type) throws Exception {
			MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
			return this.filter.match(metadataReader, this.metadataReaderFactory);
		}

	}

}
