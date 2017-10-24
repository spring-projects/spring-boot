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

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurationImportSelector}
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class AutoConfigurationImportSelectorTests {

	private final TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final MockEnvironment environment = new MockEnvironment();

	private List<AutoConfigurationImportFilter> filters = new ArrayList<>();

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.importSelector.setBeanFactory(this.beanFactory);
		this.importSelector.setEnvironment(this.environment);
		this.importSelector.setResourceLoader(new DefaultResourceLoader());
	}

	@Test
	public void importsAreSelectedWhenUsingEnableAutoConfiguration() {
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSameSizeAs(SpringFactoriesLoader.loadFactoryNames(
				EnableAutoConfiguration.class, getClass().getClassLoader()));
		assertThat(this.importSelector.getLastEvent().getExclusions()).isEmpty();
	}

	@Test
	public void classExclusionsAreApplied() {
		String[] imports = selectImports(
				EnableAutoConfigurationWithClassExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
				.contains(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void classExclusionsAreAppliedWhenUsingSpringBootApplication() {
		String[] imports = selectImports(SpringBootApplicationWithClassExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
				.contains(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void classNamesExclusionsAreApplied() {
		String[] imports = selectImports(
				EnableAutoConfigurationWithClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
				.contains(MustacheAutoConfiguration.class.getName());
	}

	@Test
	public void classNamesExclusionsAreAppliedWhenUsingSpringBootApplication() {
		String[] imports = selectImports(
				SpringBootApplicationWithClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
				.contains(MustacheAutoConfiguration.class.getName());
	}

	@Test
	public void propertyExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				FreeMarkerAutoConfiguration.class.getName());
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
				.contains(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void severalPropertyExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				FreeMarkerAutoConfiguration.class.getName() + ","
						+ MustacheAutoConfiguration.class.getName());
		testSeveralPropertyExclusionsAreApplied();
	}

	@Test
	public void severalPropertyExclusionsAreAppliedWithExtraSpaces() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				FreeMarkerAutoConfiguration.class.getName() + " , "
						+ MustacheAutoConfiguration.class.getName() + " ");
		testSeveralPropertyExclusionsAreApplied();
	}

	@Test
	public void severalPropertyYamlExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude[0]",
				FreeMarkerAutoConfiguration.class.getName());
		this.environment.setProperty("spring.autoconfigure.exclude[1]",
				MustacheAutoConfiguration.class.getName());
		testSeveralPropertyExclusionsAreApplied();
	}

	private void testSeveralPropertyExclusionsAreApplied() {
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 2);
		assertThat(this.importSelector.getLastEvent().getExclusions()).contains(
				FreeMarkerAutoConfiguration.class.getName(),
				MustacheAutoConfiguration.class.getName());
	}

	@Test
	public void combinedExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				ThymeleafAutoConfiguration.class.getName());
		String[] imports = selectImports(
				EnableAutoConfigurationWithClassAndClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 3);
		assertThat(this.importSelector.getLastEvent().getExclusions()).contains(
				FreeMarkerAutoConfiguration.class.getName(),
				MustacheAutoConfiguration.class.getName(),
				ThymeleafAutoConfiguration.class.getName());
	}

	@Test
	public void nonAutoConfigurationClassExclusionsShouldThrowException()
			throws Exception {
		this.expected.expect(IllegalStateException.class);
		selectImports(EnableAutoConfigurationWithFaultyClassExclude.class);
	}

	@Test
	public void nonAutoConfigurationClassNameExclusionsWhenPresentOnClassPathShouldThrowException()
			throws Exception {
		this.expected.expect(IllegalStateException.class);
		selectImports(EnableAutoConfigurationWithFaultyClassNameExclude.class);
	}

	@Test
	public void nonAutoConfigurationPropertyExclusionsWhenPresentOnClassPathShouldThrowException()
			throws Exception {
		this.environment.setProperty("spring.autoconfigure.exclude",
				"org.springframework.boot.autoconfigure."
						+ "AutoConfigurationImportSelectorTests.TestConfiguration");
		this.expected.expect(IllegalStateException.class);
		selectImports(BasicEnableAutoConfiguration.class);
	}

	@Test
	public void nameAndPropertyExclusionsWhenNotPresentOnClasspathShouldNotThrowException()
			throws Exception {
		this.environment.setProperty("spring.autoconfigure.exclude",
				"org.springframework.boot.autoconfigure.DoesNotExist2");
		selectImports(EnableAutoConfigurationWithAbsentClassNameExclude.class);
		assertThat(this.importSelector.getLastEvent().getExclusions())
				.containsExactlyInAnyOrder(
						"org.springframework.boot.autoconfigure.DoesNotExist1",
						"org.springframework.boot.autoconfigure.DoesNotExist2");
	}

	@Test
	public void filterShouldFilterImports() throws Exception {
		String[] defaultImports = selectImports(BasicEnableAutoConfiguration.class);
		this.filters.add(new TestAutoConfigurationImportFilter(defaultImports, 1));
		this.filters.add(new TestAutoConfigurationImportFilter(defaultImports, 3, 4));
		String[] filtered = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(filtered).hasSize(defaultImports.length - 3);
		assertThat(filtered).doesNotContain(defaultImports[1], defaultImports[3],
				defaultImports[4]);
	}

	@Test
	public void filterShouldSupportAware() throws Exception {
		TestAutoConfigurationImportFilter filter = new TestAutoConfigurationImportFilter(
				new String[] {});
		this.filters.add(filter);
		selectImports(BasicEnableAutoConfiguration.class);
		assertThat(filter.getBeanFactory()).isEqualTo(this.beanFactory);
	}

	private String[] selectImports(Class<?> source) {
		return this.importSelector.selectImports(new StandardAnnotationMetadata(source));
	}

	private List<String> getAutoConfigurationClassNames() {
		return SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
				getClass().getClassLoader());
	}

	private class TestAutoConfigurationImportSelector
			extends AutoConfigurationImportSelector {

		private AutoConfigurationImportEvent lastEvent;

		@Override
		protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
			return AutoConfigurationImportSelectorTests.this.filters;
		}

		@Override
		protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
			return Collections.singletonList((event) -> this.lastEvent = event);
		}

		public AutoConfigurationImportEvent getLastEvent() {
			return this.lastEvent;
		}

	}

	private static class TestAutoConfigurationImportFilter
			implements AutoConfigurationImportFilter, BeanFactoryAware {

		private final Set<String> nonMatching = new HashSet<>();

		private BeanFactory beanFactory;

		TestAutoConfigurationImportFilter(String[] configurations, int... nonMatching) {
			for (int i : nonMatching) {
				this.nonMatching.add(configurations[i]);
			}
		}

		@Override
		public boolean[] match(String[] autoConfigurationClasses,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			boolean[] result = new boolean[autoConfigurationClasses.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = !this.nonMatching.contains(autoConfigurationClasses[i]);
			}
			return result;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		public BeanFactory getBeanFactory() {
			return this.beanFactory;
		}

	}

	@Configuration
	private class TestConfiguration {

	}

	@EnableAutoConfiguration
	private class BasicEnableAutoConfiguration {

	}

	@EnableAutoConfiguration(exclude = FreeMarkerAutoConfiguration.class)
	private class EnableAutoConfigurationWithClassExclusions {

	}

	@SpringBootApplication(exclude = FreeMarkerAutoConfiguration.class)
	private class SpringBootApplicationWithClassExclusions {

	}

	@EnableAutoConfiguration(excludeName = "org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration")
	private class EnableAutoConfigurationWithClassNameExclusions {

	}

	@EnableAutoConfiguration(exclude = MustacheAutoConfiguration.class, excludeName = "org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration")
	private class EnableAutoConfigurationWithClassAndClassNameExclusions {

	}

	@EnableAutoConfiguration(exclude = TestConfiguration.class)
	private class EnableAutoConfigurationWithFaultyClassExclude {

	}

	@EnableAutoConfiguration(excludeName = "org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests.TestConfiguration")
	private class EnableAutoConfigurationWithFaultyClassNameExclude {

	}

	@EnableAutoConfiguration(excludeName = "org.springframework.boot.autoconfigure.DoesNotExist1")
	private class EnableAutoConfigurationWithAbsentClassNameExclude {

	}

	@SpringBootApplication(excludeName = "org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration")
	private class SpringBootApplicationWithClassNameExclusions {

	}

}
