/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NotConstructorBoundInjectionFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class NotConstructorBoundInjectionFailureAnalyzerTests {

	private final NotConstructorBoundInjectionFailureAnalyzer analyzer = new NotConstructorBoundInjectionFailureAnalyzer();

	@Test
	void failureAnalysisForConfigurationPropertiesThatShouldHaveBeenConstructorBound() {
		FailureAnalysis analysis = analyzeFailure(
				createFailure(ShouldHaveUsedConstructorBindingPropertiesConfiguration.class));
		assertThat(analysis.getDescription()).isEqualTo(ConstructorBoundProperties.class.getSimpleName()
				+ " is annotated with @" + ConstructorBinding.class.getSimpleName()
				+ " but it is defined as a regular bean which caused dependency injection to fail.");
		assertThat(analysis.getAction())
				.isEqualTo("Update your configuration so that " + ConstructorBoundProperties.class.getSimpleName()
						+ " is defined via @" + ConfigurationPropertiesScan.class.getSimpleName() + " or @"
						+ EnableConfigurationProperties.class.getSimpleName() + ".");
	}

	@Test
	void failureAnalysisForNonConstructorBoundProperties() {
		FailureAnalysis analysis = analyzeFailure(createFailure(JavaBeanBoundPropertiesConfiguration.class));
		assertThat(analysis).isNull();
	}

	private FatalBeanException createFailure(Class<?> config) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(config);
			context.refresh();
			return null;
		}
		catch (FatalBeanException ex) {
			return ex;
		}
	}

	private FailureAnalysis analyzeFailure(Exception failure) {
		assertThat(failure).isNotNull();
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		if (analysis != null) {
			new LoggingFailureAnalysisReporter().report(analysis);
		}
		return analysis;
	}

	@ConstructorBinding
	@ConfigurationProperties("test")
	static class ConstructorBoundProperties {

		private final String name;

		ConstructorBoundProperties(String name) {
			this.name = name;
		}

		String getName() {
			return this.name;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ConstructorBoundProperties.class)
	static class ShouldHaveUsedConstructorBindingPropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class JavaBeanBoundProperties {

		private String name;

		JavaBeanBoundProperties(String dependency) {

		}

		String getName() {
			return this.name;
		}

		void setName(String name) {
			this.name = name;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(JavaBeanBoundProperties.class)
	static class JavaBeanBoundPropertiesConfiguration {

	}

}
