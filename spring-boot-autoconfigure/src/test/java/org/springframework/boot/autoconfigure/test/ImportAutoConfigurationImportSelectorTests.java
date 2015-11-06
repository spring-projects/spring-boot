/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link ImportAutoConfigurationImportSelector}.
 *
 * @author Phillip Webb
 */
@RunWith(MockitoJUnitRunner.class)
public class ImportAutoConfigurationImportSelectorTests {

	private final ImportAutoConfigurationImportSelector importSelector = new ImportAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Mock
	private Environment environment;

	@Mock
	private AnnotationMetadata annotationMetadata;

	@Mock
	private AnnotationAttributes annotationAttributes;

	@Before
	public void configureImportSelector() {
		this.importSelector.setBeanFactory(this.beanFactory);
		this.importSelector.setEnvironment(this.environment);
		this.importSelector.setResourceLoader(new DefaultResourceLoader());
	}

	@Test
	public void importsAreSelected() {
		String[] value = new String[] { FreeMarkerAutoConfiguration.class.getName() };
		configureValue(value);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports, equalTo(value));
	}

	@Test
	public void propertyExclusionsAreNotApplied() {
		configureValue(new String[] { FreeMarkerAutoConfiguration.class.getName() });
		this.importSelector.selectImports(this.annotationMetadata);
		verifyZeroInteractions(this.environment);
	}

	private void configureValue(String... value) {
		String name = ImportAutoConfiguration.class.getName();
		given(this.annotationMetadata.getAnnotationAttributes(name, true))
				.willReturn(this.annotationAttributes);
		given(this.annotationAttributes.getStringArray("value")).willReturn(value);
	}

}
