/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

import kotlin.Metadata;
import org.junit.Test;
import org.spockframework.runtime.model.SpecMetadata;

import org.springframework.boot.context.annotation.DeterminableImports;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImportsContextCustomizer}.
 *
 * @author Andy Wilkinson
 */
public class ImportsContextCustomizerTests {

	@Test
	public void importSelectorsCouldUseAnyAnnotations() throws Exception {
		assertThat(new ImportsContextCustomizer(FirstImportSelectorAnnotatedClass.class))
				.isNotEqualTo(new ImportsContextCustomizer(
						SecondImportSelectorAnnotatedClass.class));
	}

	@Test
	public void determinableImportSelector() throws Exception {
		assertThat(new ImportsContextCustomizer(
				FirstDeterminableImportSelectorAnnotatedClass.class))
						.isEqualTo(new ImportsContextCustomizer(
								SecondDeterminableImportSelectorAnnotatedClass.class));
	}

	@Test
	public void customizersForTestClassesWithDifferentKotlinMetadataAreEqual() {
		assertThat(new ImportsContextCustomizer(FirstKotlinAnnotatedTestClass.class))
				.isEqualTo(new ImportsContextCustomizer(
						SecondKotlinAnnotatedTestClass.class));
	}

	@Test
	public void customizersForTestClassesWithDifferentSpockMetadataAreEqual() {
		assertThat(new ImportsContextCustomizer(FirstSpockAnnotatedTestClass.class))
				.isEqualTo(new ImportsContextCustomizer(
						SecondSpockAnnotatedTestClass.class));
	}

	@Import(TestImportSelector.class)
	@Indicator1
	static class FirstImportSelectorAnnotatedClass {

	}

	@Import(TestImportSelector.class)
	@Indicator2
	static class SecondImportSelectorAnnotatedClass {

	}

	@Import(TestDeterminableImportSelector.class)
	@Indicator1
	static class FirstDeterminableImportSelectorAnnotatedClass {

	}

	@Import(TestDeterminableImportSelector.class)
	@Indicator2
	static class SecondDeterminableImportSelectorAnnotatedClass {

	}

	@Metadata(d2 = "foo")
	static class FirstKotlinAnnotatedTestClass {

	}

	@Metadata(d2 = "bar")
	static class SecondKotlinAnnotatedTestClass {

	}

	@SpecMetadata(filename = "foo", line = 10)
	static class FirstSpockAnnotatedTestClass {

	}

	@SpecMetadata(filename = "bar", line = 10)
	static class SecondSpockAnnotatedTestClass {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Indicator1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Indicator2 {

	}

	static class TestImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata arg0) {
			return new String[] {};
		}

	}

	static class TestDeterminableImportSelector
			implements ImportSelector, DeterminableImports {

		@Override
		public String[] selectImports(AnnotationMetadata arg0) {
			return new String[] { TestConfig.class.getName() };
		}

		@Override
		public Set<Object> determineImports(AnnotationMetadata metadata) {
			return Collections.<Object>singleton(TestConfig.class.getName());
		}

	}

	@Configuration
	static class TestConfig {

	}

}
