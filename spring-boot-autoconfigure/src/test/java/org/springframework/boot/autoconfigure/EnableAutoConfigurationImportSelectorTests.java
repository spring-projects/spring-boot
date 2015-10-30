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
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.velocity.VelocityAutoConfiguration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link EnableAutoConfigurationImportSelector}
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@RunWith(MockitoJUnitRunner.class)
public class EnableAutoConfigurationImportSelectorTests {

	private final EnableAutoConfigurationImportSelector importSelector = new EnableAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final MockEnvironment environment = new MockEnvironment();

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
		configureExclusions(new String[0], new String[0], new String[0]);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports.length,
				is(equalTo(SpringFactoriesLoader
						.loadFactoryNames(EnableAutoConfiguration.class,
								getClass().getClassLoader())
						.size())));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				hasSize(0));
	}

	@Test
	public void classExclusionsAreApplied() {
		configureExclusions(new String[] { FreeMarkerAutoConfiguration.class.getName() },
				new String[0], new String[0]);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports.length,
				is(equalTo(getAutoConfigurationClassNames().size() - 1)));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				contains(FreeMarkerAutoConfiguration.class.getName()));
	}

	@Test
	public void classNamesExclusionsAreApplied() {
		configureExclusions(new String[0],
				new String[] { VelocityAutoConfiguration.class.getName() },
				new String[0]);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports.length,
				is(equalTo(getAutoConfigurationClassNames().size() - 1)));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				contains(VelocityAutoConfiguration.class.getName()));
	}

	@Test
	public void propertyExclusionsAreApplied() {
		configureExclusions(new String[0], new String[0],
				new String[] { FreeMarkerAutoConfiguration.class.getName() });
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports.length,
				is(equalTo(getAutoConfigurationClassNames().size() - 1)));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				contains(FreeMarkerAutoConfiguration.class.getName()));
	}

	@Test
	public void severalPropertyExclusionsAreApplied() {
		configureExclusions(new String[0], new String[0],
				new String[] { FreeMarkerAutoConfiguration.class.getName(),
						VelocityAutoConfiguration.class.getName() });

		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports.length,
				is(equalTo(getAutoConfigurationClassNames().size() - 2)));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				containsInAnyOrder(FreeMarkerAutoConfiguration.class.getName(),
						VelocityAutoConfiguration.class.getName()));
	}

	@Test
	public void severalPropertyYamlExclusionsAreApplied() {
		configureExclusions(new String[0], new String[0], new String[0]);
		this.environment.setProperty("spring.autoconfigure.exclude[0]",
				FreeMarkerAutoConfiguration.class.getName());
		this.environment.setProperty("spring.autoconfigure.exclude[1]",
				VelocityAutoConfiguration.class.getName());
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports.length,
				is(equalTo(getAutoConfigurationClassNames().size() - 2)));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				containsInAnyOrder(FreeMarkerAutoConfiguration.class.getName(),
						VelocityAutoConfiguration.class.getName()));
	}

	@Test
	public void combinedExclusionsAreApplied() {
		configureExclusions(new String[] { VelocityAutoConfiguration.class.getName() },
				new String[] { FreeMarkerAutoConfiguration.class.getName() },
				new String[] { ThymeleafAutoConfiguration.class.getName() });
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports.length,
				is(equalTo(getAutoConfigurationClassNames().size() - 3)));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions(),
				containsInAnyOrder(FreeMarkerAutoConfiguration.class.getName(),
						VelocityAutoConfiguration.class.getName(),
						ThymeleafAutoConfiguration.class.getName()));
	}

	private void configureExclusions(String[] classExclusion, String[] nameExclusion,
			String[] propertyExclusion) {
		given(this.annotationMetadata
				.getAnnotationAttributes(EnableAutoConfiguration.class.getName(), true))
						.willReturn(this.annotationAttributes);
		given(this.annotationAttributes.getStringArray("exclude"))
				.willReturn(classExclusion);
		given(this.annotationAttributes.getStringArray("excludeName"))
				.willReturn(nameExclusion);
		if (propertyExclusion.length > 0) {
			String value = StringUtils.arrayToCommaDelimitedString(propertyExclusion);
			this.environment.setProperty("spring.autoconfigure.exclude", value);
		}
	}

	private List<String> getAutoConfigurationClassNames() {
		return SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
				getClass().getClassLoader());
	}

}
