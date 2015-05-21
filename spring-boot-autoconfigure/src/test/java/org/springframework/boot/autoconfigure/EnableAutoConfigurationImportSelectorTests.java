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

package org.springframework.boot.autoconfigure;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link EnableAutoConfigurationImportSelector}
 *
 * @author Andy Wilkinson
 */
@RunWith(MockitoJUnitRunner.class)
public class EnableAutoConfigurationImportSelectorTests {

	private final EnableAutoConfigurationImportSelector importSelector = new EnableAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Mock
	private AnnotationMetadata annotationMetadata;

	@Mock
	private AnnotationAttributes annotationAttributes;

	@Before
	public void configureImportSelector() {
		this.importSelector.setBeanFactory(this.beanFactory);
		this.importSelector.setResourceLoader(new DefaultResourceLoader());
	}

	@Test
	public void importsAreSelected() {
		configureExclusions();
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(
				imports.length,
				is(equalTo(SpringFactoriesLoader.loadFactoryNames(
						EnableAutoConfiguration.class, getClass().getClassLoader())
						.size())));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				hasSize(0));
	}

	@Test
	public void exclusionsAreApplied() {
		configureExclusions(FreeMarkerAutoConfiguration.class.getName());
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports.length,
				is(equalTo(getAutoConfigurationClassNames().size() - 1)));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				contains(FreeMarkerAutoConfiguration.class.getName()));
	}

	private void configureExclusions(String... exclusions) {
		given(
				this.annotationMetadata.getAnnotationAttributes(
						EnableAutoConfiguration.class.getName(), true)).willReturn(
				this.annotationAttributes);
		given(this.annotationAttributes.getStringArray("exclude")).willReturn(exclusions);
	}

	private List<String> getAutoConfigurationClassNames() {
		return SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
				getClass().getClassLoader());
	}
}
