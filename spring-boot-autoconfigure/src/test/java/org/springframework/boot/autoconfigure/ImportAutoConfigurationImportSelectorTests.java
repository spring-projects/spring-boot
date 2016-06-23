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

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link ImportAutoConfigurationImportSelector}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ImportAutoConfigurationImportSelectorTests {

	private final ImportAutoConfigurationImportSelector importSelector = new ImportAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Mock
	private Environment environment;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.importSelector.setBeanFactory(this.beanFactory);
		this.importSelector.setEnvironment(this.environment);
		this.importSelector.setResourceLoader(new DefaultResourceLoader());
	}

	@Test
	public void importsAreSelected() throws Exception {
		AnnotationMetadata annotationMetadata = new SimpleMetadataReaderFactory()
				.getMetadataReader(ImportFreemarker.class.getName())
				.getAnnotationMetadata();
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsExactly(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void propertyExclusionsAreNotApplied() throws Exception {
		AnnotationMetadata annotationMetadata = new SimpleMetadataReaderFactory()
				.getMetadataReader(ImportFreemarker.class.getName())
				.getAnnotationMetadata();
		this.importSelector.selectImports(annotationMetadata);
		verifyZeroInteractions(this.environment);
	}

	@Test
	public void multipleImportsAreFound() throws Exception {
		AnnotationMetadata annotationMetadata = new SimpleMetadataReaderFactory()
				.getMetadataReader(MultipleImports.class.getName())
				.getAnnotationMetadata();
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsOnly(FreeMarkerAutoConfiguration.class.getName(),
				ThymeleafAutoConfiguration.class.getName());
	}

	@Test
	public void selfAnnotatingAnnotationDoesNotCauseStackOverflow() throws IOException {
		AnnotationMetadata annotationMetadata = new SimpleMetadataReaderFactory()
				.getMetadataReader(ImportWithSelfAnnotatingAnnotation.class.getName())
				.getAnnotationMetadata();
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsOnly(ThymeleafAutoConfiguration.class.getName());
	}

	@ImportAutoConfiguration(FreeMarkerAutoConfiguration.class)
	static class ImportFreemarker {

	}

	@ImportOne
	@ImportTwo
	static class MultipleImports {

	}

	@SelfAnnotating
	static class ImportWithSelfAnnotatingAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImportAutoConfiguration(FreeMarkerAutoConfiguration.class)
	static @interface ImportOne {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
	static @interface ImportTwo {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
	@SelfAnnotating
	static @interface SelfAnnotating {

	}

}
