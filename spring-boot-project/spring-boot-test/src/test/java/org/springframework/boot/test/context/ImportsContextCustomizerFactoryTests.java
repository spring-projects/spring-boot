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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ImportsContextCustomizerFactory} and {@link ImportsContextCustomizer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ImportsContextCustomizerFactoryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ImportsContextCustomizerFactory factory = new ImportsContextCustomizerFactory();

	@Test
	public void getContextCustomizerWhenHasNoImportAnnotationShouldReturnNull() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(TestWithNoImport.class, null);
		assertThat(customizer).isNull();
	}

	@Test
	public void getContextCustomizerWhenHasImportAnnotationShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(TestWithImport.class, null);
		assertThat(customizer).isNotNull();
	}

	@Test
	public void getContextCustomizerWhenHasMetaImportAnnotationShouldReturnCustomizer() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(TestWithMetaImport.class, null);
		assertThat(customizer).isNotNull();
	}

	@Test
	public void contextCustomizerEqualsAndHashCode() {
		ContextCustomizer customizer1 = this.factory
				.createContextCustomizer(TestWithImport.class, null);
		ContextCustomizer customizer2 = this.factory
				.createContextCustomizer(TestWithImport.class, null);
		ContextCustomizer customizer3 = this.factory
				.createContextCustomizer(TestWithImportAndMetaImport.class, null);
		ContextCustomizer customizer4 = this.factory
				.createContextCustomizer(TestWithSameImportAndMetaImport.class, null);
		assertThat(customizer1.hashCode()).isEqualTo(customizer1.hashCode());
		assertThat(customizer1.hashCode()).isEqualTo(customizer2.hashCode());
		assertThat(customizer1).isEqualTo(customizer1).isEqualTo(customizer2)
				.isNotEqualTo(customizer3);
		assertThat(customizer3).isEqualTo(customizer4);
	}

	@Test
	public void getContextCustomizerWhenClassHasBeanMethodsShouldThrowException() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Test classes cannot include @Bean methods");
		this.factory.createContextCustomizer(TestWithImportAndBeanMethod.class, null);
	}

	@Test
	public void contextCustomizerImportsBeans() {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(TestWithImport.class, null);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		customizer.customizeContext(context, mock(MergedContextConfiguration.class));
		context.refresh();
		assertThat(context.getBean(ImportedBean.class)).isNotNull();
	}

	@Test
	public void selfAnnotatingAnnotationDoesNotCauseStackOverflow() {
		assertThat(this.factory.createContextCustomizer(
				TestWithImportAndSelfAnnotatingAnnotation.class, null)).isNotNull();
	}

	static class TestWithNoImport {

	}

	@Import(ImportedBean.class)
	static class TestWithImport {

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

	@Import(ImportedBean.class)
	static class TestWithImportAndBeanMethod {

		@Bean
		public String bean() {
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
	static @interface SelfAnnotating {

	}

}
