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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.context.annotation.DeferredImportSelector.Group.Entry;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AutoConfigurationImportSelector}
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
@WithResource(name = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports", content = """
		com.example.one.FirstAutoConfiguration
		com.example.two.SecondAutoConfiguration
		com.example.three.ThirdAutoConfiguration
		com.example.four.FourthAutoConfiguration
		com.example.five.FifthAutoConfiguration
		com.example.six.SixthAutoConfiguration
		org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests$SeventhAutoConfiguration
		""")
class AutoConfigurationImportSelectorTests {

	private final TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector(null);

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final MockEnvironment environment = new MockEnvironment();

	private final List<AutoConfigurationImportFilter> filters = new ArrayList<>();

	@BeforeEach
	void setup() {
		setupImportSelector(this.importSelector);
	}

	@Test
	void importsAreSelectedWhenUsingEnableAutoConfiguration() {
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSameSizeAs(getAutoConfigurationClassNames());
		assertThat(getLastEvent().getExclusions()).isEmpty();
	}

	@Test
	void classExclusionsAreApplied() {
		String[] imports = selectImports(EnableAutoConfigurationWithClassExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(getLastEvent().getExclusions()).contains(SeventhAutoConfiguration.class.getName());
	}

	@Test
	void classExclusionsAreAppliedWhenUsingSpringBootApplication() {
		String[] imports = selectImports(SpringBootApplicationWithClassExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(getLastEvent().getExclusions()).contains(SeventhAutoConfiguration.class.getName());
	}

	@Test
	void classNamesExclusionsAreApplied() {
		String[] imports = selectImports(EnableAutoConfigurationWithClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(getLastEvent().getExclusions()).contains("com.example.one.FirstAutoConfiguration");
	}

	@Test
	void classNamesExclusionsAreAppliedWhenUsingSpringBootApplication() {
		String[] imports = selectImports(SpringBootApplicationWithClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(getLastEvent().getExclusions()).contains("com.example.three.ThirdAutoConfiguration");
	}

	@Test
	void propertyExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude", "com.example.three.ThirdAutoConfiguration");
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(getLastEvent().getExclusions()).contains("com.example.three.ThirdAutoConfiguration");
	}

	@Test
	void severalPropertyExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				"com.example.two.SecondAutoConfiguration,com.example.four.FourthAutoConfiguration");
		testSeveralPropertyExclusionsAreApplied();
	}

	@Test
	void severalPropertyExclusionsAreAppliedWithExtraSpaces() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				"com.example.two.SecondAutoConfiguration , com.example.four.FourthAutoConfiguration  ");
		testSeveralPropertyExclusionsAreApplied();
	}

	@Test
	void severalPropertyYamlExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude[0]", "com.example.two.SecondAutoConfiguration");
		this.environment.setProperty("spring.autoconfigure.exclude[1]", "com.example.four.FourthAutoConfiguration");
		testSeveralPropertyExclusionsAreApplied();
	}

	private void testSeveralPropertyExclusionsAreApplied() {
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 2);
		assertThat(getLastEvent().getExclusions()).contains("com.example.two.SecondAutoConfiguration",
				"com.example.four.FourthAutoConfiguration");
	}

	@Test
	void combinedExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude", "com.example.one.FirstAutoConfiguration");
		String[] imports = selectImports(EnableAutoConfigurationWithClassAndClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 3);
		assertThat(getLastEvent().getExclusions()).contains("com.example.one.FirstAutoConfiguration",
				"com.example.five.FifthAutoConfiguration", SeventhAutoConfiguration.class.getName());
	}

	@Test
	@WithTestAutoConfigurationImportsResource
	@WithTestAutoConfigurationReplacementsResource
	void removedExclusionsAreApplied() {
		TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector(
				TestAutoConfiguration.class);
		setupImportSelector(importSelector);
		AnnotationMetadata metadata = AnnotationMetadata.introspect(BasicEnableAutoConfiguration.class);
		assertThat(importSelector.selectImports(metadata)).contains(ReplacementAutoConfiguration.class.getName());
		this.environment.setProperty("spring.autoconfigure.exclude", DeprecatedAutoConfiguration.class.getName());
		assertThat(importSelector.selectImports(metadata)).doesNotContain(ReplacementAutoConfiguration.class.getName());
	}

	@Test
	void nonAutoConfigurationClassExclusionsShouldThrowException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> selectImports(EnableAutoConfigurationWithFaultyClassExclude.class));
	}

	@Test
	void nonAutoConfigurationClassNameExclusionsWhenPresentOnClassPathShouldThrowException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> selectImports(EnableAutoConfigurationWithFaultyClassNameExclude.class));
	}

	@Test
	void nonAutoConfigurationPropertyExclusionsWhenPresentOnClassPathShouldThrowException() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				"org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests.TestConfiguration");
		assertThatIllegalStateException().isThrownBy(() -> selectImports(BasicEnableAutoConfiguration.class));
	}

	@Test
	void nameAndPropertyExclusionsWhenNotPresentOnClasspathShouldNotThrowException() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				"org.springframework.boot.autoconfigure.DoesNotExist2");
		selectImports(EnableAutoConfigurationWithAbsentClassNameExclude.class);
		assertThat(getLastEvent().getExclusions()).containsExactlyInAnyOrder(
				"org.springframework.boot.autoconfigure.DoesNotExist1",
				"org.springframework.boot.autoconfigure.DoesNotExist2");
	}

	@Test
	void filterShouldFilterImports() {
		String[] defaultImports = selectImports(BasicEnableAutoConfiguration.class);
		this.filters.add(new TestAutoConfigurationImportFilter(defaultImports, 1));
		this.filters.add(new TestAutoConfigurationImportFilter(defaultImports, 3, 4));
		String[] filtered = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(filtered).hasSize(defaultImports.length - 3);
		assertThat(filtered).doesNotContain(defaultImports[1], defaultImports[3], defaultImports[4]);
	}

	@Test
	void filterShouldSupportAware() {
		TestAutoConfigurationImportFilter filter = new TestAutoConfigurationImportFilter(new String[] {});
		this.filters.add(filter);
		selectImports(BasicEnableAutoConfiguration.class);
		assertThat(filter.getBeanFactory()).isEqualTo(this.beanFactory);
	}

	@Test
	void getExclusionFilterReuseFilters() {
		String[] allImports = new String[] { "com.example.A", "com.example.B", "com.example.C" };
		this.filters.add(new TestAutoConfigurationImportFilter(allImports, 0));
		this.filters.add(new TestAutoConfigurationImportFilter(allImports, 2));
		assertThat(this.importSelector.getExclusionFilter().test("com.example.A")).isTrue();
		assertThat(this.importSelector.getExclusionFilter().test("com.example.B")).isFalse();
		assertThat(this.importSelector.getExclusionFilter().test("com.example.C")).isTrue();
	}

	@Test
	@WithTestAutoConfigurationImportsResource
	@WithTestAutoConfigurationReplacementsResource
	void sortingConsidersReplacements() {
		TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector(
				TestAutoConfiguration.class);
		setupImportSelector(importSelector);
		AnnotationMetadata metadata = AnnotationMetadata.introspect(BasicEnableAutoConfiguration.class);
		assertThat(importSelector.selectImports(metadata)).containsExactly(
				AfterDeprecatedAutoConfiguration.class.getName(), ReplacementAutoConfiguration.class.getName());
		Group group = BeanUtils.instantiateClass(importSelector.getImportGroup());
		((BeanFactoryAware) group).setBeanFactory(this.beanFactory);
		group.process(metadata, importSelector);
		Stream<Entry> imports = StreamSupport.stream(group.selectImports().spliterator(), false);
		assertThat(imports.map(Entry::getImportClassName)).containsExactly(ReplacementAutoConfiguration.class.getName(),
				AfterDeprecatedAutoConfiguration.class.getName());
	}

	private String[] selectImports(Class<?> source) {
		return this.importSelector.selectImports(AnnotationMetadata.introspect(source));
	}

	private List<String> getAutoConfigurationClassNames() {
		return ImportCandidates.load(AutoConfiguration.class, Thread.currentThread().getContextClassLoader())
			.getCandidates();
	}

	private void setupImportSelector(TestAutoConfigurationImportSelector importSelector) {
		importSelector.setBeanFactory(this.beanFactory);
		importSelector.setEnvironment(this.environment);
		importSelector.setResourceLoader(new DefaultResourceLoader());
		importSelector.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
	}

	private AutoConfigurationImportEvent getLastEvent() {
		AutoConfigurationImportEvent result = this.importSelector.getLastEvent();
		assertThat(result).isNotNull();
		return result;
	}

	private final class TestAutoConfigurationImportSelector extends AutoConfigurationImportSelector {

		private @Nullable AutoConfigurationImportEvent lastEvent;

		TestAutoConfigurationImportSelector(@Nullable Class<?> autoConfigurationAnnotation) {
			super(autoConfigurationAnnotation);
		}

		@Override
		protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
			return AutoConfigurationImportSelectorTests.this.filters;
		}

		@Override
		protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
			return Collections.singletonList((event) -> this.lastEvent = event);
		}

		@Nullable AutoConfigurationImportEvent getLastEvent() {
			return this.lastEvent;
		}

	}

	static class TestAutoConfigurationImportFilter implements AutoConfigurationImportFilter, BeanFactoryAware {

		private final Set<String> nonMatching = new HashSet<>();

		@SuppressWarnings("NullAway.Init")
		private BeanFactory beanFactory;

		TestAutoConfigurationImportFilter(String[] configurations, int... nonMatching) {
			for (int i : nonMatching) {
				this.nonMatching.add(configurations[i]);
			}
		}

		@Override
		public boolean[] match(@Nullable String[] autoConfigurationClasses,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			boolean[] result = new boolean[autoConfigurationClasses.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = !this.nonMatching.contains(autoConfigurationClasses[i]);
			}
			return result;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		BeanFactory getBeanFactory() {
			return this.beanFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	private final class TestConfiguration {

	}

	@EnableAutoConfiguration
	private final class BasicEnableAutoConfiguration {

	}

	@EnableAutoConfiguration(exclude = SeventhAutoConfiguration.class)
	private final class EnableAutoConfigurationWithClassExclusions {

	}

	@SpringBootApplication(exclude = SeventhAutoConfiguration.class)
	private final class SpringBootApplicationWithClassExclusions {

	}

	@EnableAutoConfiguration(excludeName = "com.example.one.FirstAutoConfiguration")
	private final class EnableAutoConfigurationWithClassNameExclusions {

	}

	@EnableAutoConfiguration(exclude = SeventhAutoConfiguration.class,
			excludeName = "com.example.five.FifthAutoConfiguration")
	private final class EnableAutoConfigurationWithClassAndClassNameExclusions {

	}

	@EnableAutoConfiguration(exclude = TestConfiguration.class)
	private final class EnableAutoConfigurationWithFaultyClassExclude {

	}

	@EnableAutoConfiguration(
			excludeName = "org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests.TestConfiguration")
	private final class EnableAutoConfigurationWithFaultyClassNameExclude {

	}

	@EnableAutoConfiguration(excludeName = "org.springframework.boot.autoconfigure.DoesNotExist1")
	private final class EnableAutoConfigurationWithAbsentClassNameExclude {

	}

	@SpringBootApplication(excludeName = "com.example.three.ThirdAutoConfiguration")
	private final class SpringBootApplicationWithClassNameExclusions {

	}

	static class DeprecatedAutoConfiguration {

	}

	static class ReplacementAutoConfiguration {

	}

	@AutoConfigureAfter(DeprecatedAutoConfiguration.class)
	static class AfterDeprecatedAutoConfiguration {

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAutoConfiguration {

	}

	@AutoConfiguration
	public static final class SeventhAutoConfiguration {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(
			name = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests$TestAutoConfiguration.imports",
			content = """
					org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests$AfterDeprecatedAutoConfiguration
					org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests$ReplacementAutoConfiguration
					""")
	@interface WithTestAutoConfigurationImportsResource {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(
			name = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests$TestAutoConfiguration.replacements",
			content = """
					org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests$DeprecatedAutoConfiguration=\
					org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests$ReplacementAutoConfiguration
					""")
	@interface WithTestAutoConfigurationReplacementsResource {

	}

}
