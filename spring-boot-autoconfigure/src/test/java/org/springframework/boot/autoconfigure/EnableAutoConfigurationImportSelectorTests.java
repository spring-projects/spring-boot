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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link EnableAutoConfigurationImportSelector}
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 *
 */
@SuppressWarnings("deprecation")
public class EnableAutoConfigurationImportSelectorTests {

	private final EnableAutoConfigurationImportSelector importSelector = new EnableAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final MockEnvironment environment = new MockEnvironment();

	@Mock
	private AnnotationMetadata annotationMetadata;

	@Mock
	private AnnotationAttributes annotationAttributes;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.importSelector.setBeanFactory(this.beanFactory);
		this.importSelector.setEnvironment(this.environment);
		this.importSelector.setResourceLoader(new DefaultResourceLoader());
	}

	@Test
	public void importsAreSelected() {
		configureExclusions(new String[0], new String[0], new String[0]);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).hasSameSizeAs(SpringFactoriesLoader.loadFactoryNames(
				EnableAutoConfiguration.class, getClass().getClassLoader()));
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions())
				.isEmpty();
	}

	@Test
	public void classExclusionsAreApplied() {
		configureExclusions(new String[] { FreeMarkerAutoConfiguration.class.getName() },
				new String[0], new String[0]);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions())
				.contains(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void classNamesExclusionsAreApplied() {
		configureExclusions(new String[0],
				new String[] { VelocityAutoConfiguration.class.getName() },
				new String[0]);
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions())
				.contains(VelocityAutoConfiguration.class.getName());
	}

	@Test
	public void propertyExclusionsAreApplied() {
		configureExclusions(new String[0], new String[0],
				new String[] { FreeMarkerAutoConfiguration.class.getName() });
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions())
				.contains(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void severalPropertyExclusionsAreApplied() {
		configureExclusions(new String[0], new String[0],
				new String[] { FreeMarkerAutoConfiguration.class.getName(),
						VelocityAutoConfiguration.class.getName() });
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 2);
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions())
				.contains(FreeMarkerAutoConfiguration.class.getName(),
						VelocityAutoConfiguration.class.getName());
	}

	@Test
	public void severalPropertyYamlExclusionsAreApplied() {
		configureExclusions(new String[0], new String[0], new String[0]);
		this.environment.setProperty("spring.autoconfigure.exclude[0]",
				FreeMarkerAutoConfiguration.class.getName());
		this.environment.setProperty("spring.autoconfigure.exclude[1]",
				VelocityAutoConfiguration.class.getName());
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 2);
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions())
				.contains(FreeMarkerAutoConfiguration.class.getName(),
						VelocityAutoConfiguration.class.getName());
	}

	@Test
	public void combinedExclusionsAreApplied() {
		configureExclusions(new String[] { VelocityAutoConfiguration.class.getName() },
				new String[] { FreeMarkerAutoConfiguration.class.getName() },
				new String[] { ThymeleafAutoConfiguration.class.getName() });
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 3);
		assertThat(ConditionEvaluationReport.get(this.beanFactory).getExclusions())
				.contains(FreeMarkerAutoConfiguration.class.getName(),
						VelocityAutoConfiguration.class.getName(),
						ThymeleafAutoConfiguration.class.getName());
	}

	@Test
	public void propertyOverrideSetToTrue() throws Exception {
		configureExclusions(new String[0], new String[0], new String[0]);
		this.environment.setProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY,
				"true");
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).isNotEmpty();
	}

	@Test
	public void propertyOverrideSetToFalse() throws Exception {
		configureExclusions(new String[0], new String[0], new String[0]);
		this.environment.setProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY,
				"false");
		String[] imports = this.importSelector.selectImports(this.annotationMetadata);
		assertThat(imports).isEmpty();
	}

	private void configureExclusions(String[] classExclusion, String[] nameExclusion,
			String[] propertyExclusion) {
		String annotationName = EnableAutoConfiguration.class.getName();
		given(this.annotationMetadata.isAnnotated(annotationName)).willReturn(true);
		given(this.annotationMetadata.getAnnotationAttributes(annotationName, true))
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
