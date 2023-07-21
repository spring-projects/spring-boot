/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.diagnostics.analyzer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchBeanDefinitionFailureAnalyzer}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class NoSuchBeanDefinitionFailureAnalyzerTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private final NoSuchBeanDefinitionFailureAnalyzer analyzer = new NoSuchBeanDefinitionFailureAnalyzer(
			this.context.getBeanFactory());

	@Test
	void failureAnalysisForMultipleBeans() {
		FailureAnalysis analysis = analyzeFailure(new NoUniqueBeanDefinitionException(String.class, 2, "Test"));
		assertThat(analysis).isNull();
	}

	@Test
	void failureAnalysisForNoMatchType() {
		FailureAnalysis analysis = analyzeFailure(createFailure(StringHandler.class));
		assertDescriptionConstructorMissingType(analysis, StringHandler.class, 0, String.class);
		assertThat(analysis.getDescription())
			.doesNotContain("No matching auto-configuration has been found for this type.");
		assertThat(analysis.getAction()).startsWith(
				String.format("Consider defining a bean of type '%s' in your configuration.", String.class.getName()));
	}

	@Test
	void failureAnalysisForMissingPropertyExactType() {
		FailureAnalysis analysis = analyzeFailure(createFailure(StringPropertyTypeConfiguration.class));
		assertDescriptionConstructorMissingType(analysis, StringHandler.class, 0, String.class);
		assertBeanMethodDisabled(analysis, "did not find property 'spring.string.enabled'",
				TestPropertyAutoConfiguration.class, "string");
		assertActionMissingType(analysis, String.class);
	}

	@Test
	void failureAnalysisForMissingPropertySubType() {
		FailureAnalysis analysis = analyzeFailure(createFailure(IntegerPropertyTypeConfiguration.class));
		assertThat(analysis).isNotNull();
		assertDescriptionConstructorMissingType(analysis, NumberHandler.class, 0, Number.class);
		assertBeanMethodDisabled(analysis, "did not find property 'spring.integer.enabled'",
				TestPropertyAutoConfiguration.class, "integer");
		assertActionMissingType(analysis, Number.class);
	}

	@Test
	void failureAnalysisForMissingClassOnAutoConfigurationType() {
		FailureAnalysis analysis = analyzeFailure(createFailure(MissingClassOnAutoConfigurationConfiguration.class));
		assertDescriptionConstructorMissingType(analysis, StringHandler.class, 0, String.class);
		assertClassDisabled(analysis, "did not find required class 'com.example.FooBar'", "string",
				ClassUtils.getShortName(TestTypeClassAutoConfiguration.class));
		assertActionMissingType(analysis, String.class);
	}

	@Test
	void failureAnalysisForExcludedAutoConfigurationType() {
		FatalBeanException failure = createFailure(StringHandler.class);
		addExclusions(this.analyzer, TestPropertyAutoConfiguration.class);
		FailureAnalysis analysis = analyzeFailure(failure);
		assertDescriptionConstructorMissingType(analysis, StringHandler.class, 0, String.class);
		String configClass = ClassUtils.getShortName(TestPropertyAutoConfiguration.class.getName());
		assertClassDisabled(analysis, String.format("auto-configuration '%s' was excluded", configClass), "string",
				ClassUtils.getShortName(TestPropertyAutoConfiguration.class));
		assertActionMissingType(analysis, String.class);
	}

	@Test
	void failureAnalysisForSeveralConditionsType() {
		FailureAnalysis analysis = analyzeFailure(createFailure(SeveralAutoConfigurationTypeConfiguration.class));
		assertDescriptionConstructorMissingType(analysis, StringHandler.class, 0, String.class);
		assertBeanMethodDisabled(analysis, "did not find property 'spring.string.enabled'",
				TestPropertyAutoConfiguration.class, "string");
		assertClassDisabled(analysis, "did not find required class 'com.example.FooBar'", "string",
				ClassUtils.getShortName(TestPropertyAutoConfiguration.class));
		assertActionMissingType(analysis, String.class);
	}

	@Test
	void failureAnalysisForNoMatchName() {
		FailureAnalysis analysis = analyzeFailure(createFailure(StringNameHandler.class));
		assertThat(analysis.getDescription())
			.startsWith(String.format("Constructor in %s required a bean named '%s' that could not be found",
					StringNameHandler.class.getName(), "test-string"));
		assertThat(analysis.getAction())
			.startsWith(String.format("Consider defining a bean named '%s' in your configuration.", "test-string"));
	}

	@Test
	void failureAnalysisForMissingBeanName() {
		FailureAnalysis analysis = analyzeFailure(createFailure(StringMissingBeanNameConfiguration.class));
		assertThat(analysis.getDescription())
			.startsWith(String.format("Constructor in %s required a bean named '%s' that could not be found",
					StringNameHandler.class.getName(), "test-string"));
		assertBeanMethodDisabled(analysis,
				"@ConditionalOnBean (types: java.lang.Integer; SearchStrategy: all) did not find any beans",
				TestMissingBeanAutoConfiguration.class, "string");
		assertActionMissingName(analysis, "test-string");
	}

	@Test
	void failureAnalysisForNullBeanByType() {
		FailureAnalysis analysis = analyzeFailure(createFailure(StringNullBeanConfiguration.class));
		assertDescriptionConstructorMissingType(analysis, StringHandler.class, 0, String.class);
		assertUserDefinedBean(analysis, "as the bean value is null", TestNullBeanConfiguration.class, "string");
		assertActionMissingType(analysis, String.class);
	}

	@Test
	void failureAnalysisForUnmatchedQualifier() {
		FailureAnalysis analysis = analyzeFailure(createFailure(QualifiedBeanConfiguration.class));
		assertThat(analysis.getDescription())
			.containsPattern("@org.springframework.beans.factory.annotation.Qualifier\\(\"*alpha\"*\\)");
	}

	private void assertDescriptionConstructorMissingType(FailureAnalysis analysis, Class<?> component, int index,
			Class<?> type) {
		String expected = String.format(
				"Parameter %s of constructor in %s required a bean of type '%s' that could not be found.", index,
				component.getName(), type.getName());
		assertThat(analysis.getDescription()).startsWith(expected);
	}

	private void assertActionMissingType(FailureAnalysis analysis, Class<?> type) {
		assertThat(analysis.getAction()).startsWith(String.format(
				"Consider revisiting the entries above or defining a bean of type '%s' in your configuration.",
				type.getName()));
		assertThat(analysis.getAction()).doesNotContain("@ConstructorBinding");
	}

	private void assertActionMissingName(FailureAnalysis analysis, String name) {
		assertThat(analysis.getAction()).startsWith(String.format(
				"Consider revisiting the entries above or defining a bean named '%s' in your configuration.", name));
	}

	private void assertBeanMethodDisabled(FailureAnalysis analysis, String description, Class<?> target,
			String methodName) {
		String expected = String.format("Bean method '%s' in '%s' not loaded because", methodName,
				ClassUtils.getShortName(target));
		assertThat(analysis.getDescription()).contains(expected);
		assertThat(analysis.getDescription()).contains(description);
	}

	private void assertClassDisabled(FailureAnalysis analysis, String description, String methodName,
			String className) {
		String expected = String.format("Bean method '%s' in '%s' not loaded because", methodName, className);
		assertThat(analysis.getDescription()).contains(expected);
		assertThat(analysis.getDescription()).contains(description);
	}

	private void assertUserDefinedBean(FailureAnalysis analysis, String description, Class<?> target,
			String methodName) {
		String expected = String.format("User-defined bean method '%s' in '%s' ignored", methodName,
				ClassUtils.getShortName(target));
		assertThat(analysis.getDescription()).contains(expected);
		assertThat(analysis.getDescription()).contains(description);
	}

	private static void addExclusions(NoSuchBeanDefinitionFailureAnalyzer analyzer, Class<?>... classes) {
		ConditionEvaluationReport report = (ConditionEvaluationReport) ReflectionTestUtils.getField(analyzer, "report");
		List<String> exclusions = new ArrayList<>(report.getExclusions());
		for (Class<?> c : classes) {
			exclusions.add(c.getName());
		}
		report.recordExclusions(exclusions);
	}

	private FatalBeanException createFailure(Class<?> config, String... environment) {
		try {
			TestPropertyValues.of(environment).applyTo(this.context);
			this.context.register(config);
			this.context.refresh();
			return null;
		}
		catch (FatalBeanException ex) {
			return ex;
		}
	}

	private FailureAnalysis analyzeFailure(Exception failure) {
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		if (analysis != null) {
			new LoggingFailureAnalysisReporter().report(analysis);
		}
		return analysis;
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(TestPropertyAutoConfiguration.class)
	@Import(StringHandler.class)
	static class StringPropertyTypeConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(TestPropertyAutoConfiguration.class)
	@Import(NumberHandler.class)
	static class IntegerPropertyTypeConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(TestTypeClassAutoConfiguration.class)
	@Import(StringHandler.class)
	static class MissingClassOnAutoConfigurationConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ TestPropertyAutoConfiguration.class, TestTypeClassAutoConfiguration.class })
	@Import(StringHandler.class)
	static class SeveralAutoConfigurationTypeConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(TestMissingBeanAutoConfiguration.class)
	@Import(StringNameHandler.class)
	static class StringMissingBeanNameConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(TestNullBeanConfiguration.class)
	@Import(StringHandler.class)
	static class StringNullBeanConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TestPropertyAutoConfiguration {

		@ConditionalOnProperty("spring.string.enabled")
		@Bean
		String string() {
			return "Test";
		}

		@ConditionalOnProperty("spring.integer.enabled")
		@Bean
		Integer integer() {
			return 42;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "com.example.FooBar")
	static class TestTypeClassAutoConfiguration {

		@Bean
		String string() {
			return "Test";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestMissingBeanAutoConfiguration {

		@ConditionalOnBean(Integer.class)
		@Bean(name = "test-string")
		String string() {
			return "Test";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestNullBeanConfiguration {

		@Bean
		String string() {
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class QualifiedBeanConfiguration {

		@Bean
		String consumer(@Qualifier("alpha") Thing thing) {
			return "consumer";
		}

		@Bean
		Thing producer() {
			return new Thing();
		}

		class Thing {

		}

	}

	static class StringHandler {

		StringHandler(String foo) {
		}

	}

	static class NumberHandler {

		NumberHandler(Number foo) {
		}

	}

	static class StringNameHandler {

		StringNameHandler(BeanFactory beanFactory) {
			beanFactory.getBean("test-string");
		}

	}

}
