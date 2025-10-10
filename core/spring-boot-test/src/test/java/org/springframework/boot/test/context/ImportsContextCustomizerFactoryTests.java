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

package org.springframework.boot.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ImportsContextCustomizerFactory} and {@link ImportsContextCustomizer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ImportsContextCustomizerFactoryTests {

	private final ImportsContextCustomizerFactory factory = new ImportsContextCustomizerFactory();

	@Test
	void getContextCustomizerWhenHasNoImportAnnotationShouldReturnNull() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(TestWithNoImport.class,
				Collections.emptyList());
		assertThat(customizer).isNull();
	}

	@Test
	void getContextCustomizerWhenHasImportAnnotationShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(TestWithImport.class,
				Collections.emptyList());
		assertThat(customizer).isNotNull();
	}

	@Test
	void getContextCustomizerWhenHasMetaImportAnnotationShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(TestWithMetaImport.class,
				Collections.emptyList());
		assertThat(customizer).isNotNull();
	}

	@Test
	void contextCustomizerEqualsAndHashCode() {
		ContextCustomizer customizer1 = this.factory.createContextCustomizer(TestWithImport.class,
				Collections.emptyList());
		ContextCustomizer customizer2 = this.factory.createContextCustomizer(TestWithImport.class,
				Collections.emptyList());
		ContextCustomizer customizer3 = this.factory.createContextCustomizer(TestWithImportAndMetaImport.class,
				Collections.emptyList());
		ContextCustomizer customizer4 = this.factory.createContextCustomizer(TestWithSameImportAndMetaImport.class,
				Collections.emptyList());
		assertThat(customizer1).hasSameHashCodeAs(customizer1);
		assertThat(customizer1).hasSameHashCodeAs(customizer2);
		assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2).isNotEqualTo(customizer3);
		assertThat(customizer3).isEqualTo(customizer4);
	}

	@Test
	void contextCustomizerEqualsAndHashCodeConsidersComponentScan() {
		ContextCustomizer customizer1 = this.factory
			.createContextCustomizer(TestWithImportAndComponentScanOfSomePackage.class, Collections.emptyList());
		ContextCustomizer customizer2 = this.factory
			.createContextCustomizer(TestWithImportAndComponentScanOfSomePackage.class, Collections.emptyList());
		ContextCustomizer customizer3 = this.factory
			.createContextCustomizer(TestWithImportAndComponentScanOfAnotherPackage.class, Collections.emptyList());
		assertThat(customizer1).isEqualTo(customizer2);
		assertThat(customizer1).hasSameHashCodeAs(customizer2);
		assertThat(customizer3).isNotEqualTo(customizer2).isNotEqualTo(customizer1);
		assertThat(customizer3).doesNotHaveSameHashCodeAs(customizer2).doesNotHaveSameHashCodeAs(customizer1);
	}

	@Test
	void getContextCustomizerWhenClassHasBeanMethodsShouldThrowException() {
		assertThatIllegalStateException().isThrownBy(
				() -> this.factory.createContextCustomizer(TestWithImportAndBeanMethod.class, Collections.emptyList()))
			.withMessageContaining("Test classes cannot include @Bean methods");
	}

	@Test
	void contextCustomizerImportsBeans() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(TestWithImport.class,
				Collections.emptyList());
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		assertThat(customizer).isNotNull();
		customizer.customizeContext(context, mock(MergedContextConfiguration.class));
		context.refresh();
		assertThat(context.getBean(ImportedBean.class)).isNotNull();
	}

	@Test
	void selfAnnotatingAnnotationDoesNotCauseStackOverflow() {
		assertThat(this.factory.createContextCustomizer(TestWithImportAndSelfAnnotatingAnnotation.class,
				Collections.emptyList()))
			.isNotNull();
	}

	static class TestWithNoImport {

	}

	@Import(ImportedBean.class)
	static class TestWithImport {

	}

	@Import(ImportedBean.class)
	@ComponentScan("some.package")
	static class TestWithImportAndComponentScanOfSomePackage {

	}

	@Import(ImportedBean.class)
	@ComponentScan("another.package")
	static class TestWithImportAndComponentScanOfAnotherPackage {

	}

	@MetaImport
	static class TestWithMetaImport {

	}

	@MetaImport
	@Import(AnotherImportedBean.class)
	static class TestWithImportAndMetaImport {

	}

	@MetaImport
	@Import(AnotherImportedBean.class)
	static class TestWithSameImportAndMetaImport {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ImportedBean.class)
	static class TestWithImportAndBeanMethod {

		@Bean
		String bean() {
			return "bean";
		}

	}

	@SelfAnnotating
	@Import(ImportedBean.class)
	static class TestWithImportAndSelfAnnotatingAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Import(ImportedBean.class)
	@interface MetaImport {

	}

	@Component
	static class ImportedBean {

	}

	@Component
	static class AnotherImportedBean {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@SelfAnnotating
	@interface SelfAnnotating {

	}

}
