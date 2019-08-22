/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.InvalidConfigurationPropertiesException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InvalidConfigurationPropertiesFailureAnalyzer}
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
class InvalidConfigurationPropertiesFailureAnalyzerTests {

	private final InvalidConfigurationPropertiesFailureAnalyzer analyzer = new InvalidConfigurationPropertiesFailureAnalyzer();

	@Test
	void analysisForInvalidConfigurationOfConfigurationProperties() {
		FailureAnalysis analysis = performAnalysis(TestProperties.class);
		assertThat(analysis.getDescription()).isEqualTo(getBasicDescription(TestProperties.class));
		assertThat(analysis.getAction()).isEqualTo(getBasicAction(TestProperties.class));
	}

	@Test
	void analysisForInvalidConfigurationOfConfigurationPropertiesWithSingleConstructor() {
		FailureAnalysis analysis = performAnalysis(TestPropertiesWithSingleConstructor.class);
		assertThat(analysis.getDescription()).containsSequence(
				getBasicDescription(TestPropertiesWithSingleConstructor.class),
				" Also, autowiring by constructor is enabled for "
						+ TestPropertiesWithSingleConstructor.class.getSimpleName()
						+ " which conflicts with properties constructor binding.");
		assertThat(analysis.getAction()).containsSubsequence("Consider refactoring TestPropertiesWithSingleConstructor "
				+ "so that it does not rely on other beans. Alternatively, a default constructor should be added and "
				+ "@Autowired should be defined on "
				+ "org.springframework.boot.diagnostics.analyzer.InvalidConfigurationPropertiesFailureAnalyzerTests$TestPropertiesWithSingleConstructor(java.lang.Object,java.util.function.Function<java.lang.String, java.lang.Integer>).",
				getBasicAction(TestPropertiesWithSingleConstructor.class));
	}

	@Test
	void analysisForInvalidConfigurationOfConfigurationPropertiesWithSeveralConstructors() {
		FailureAnalysis analysis = performAnalysis(TestPropertiesWithSeveralConstructors.class);
		assertThat(analysis.getDescription())
				.isEqualTo(getBasicDescription(TestPropertiesWithSeveralConstructors.class));
		assertThat(analysis.getAction()).isEqualTo(getBasicAction(TestPropertiesWithSeveralConstructors.class));
	}

	private String getBasicDescription(Class<?> target) {
		return target.getSimpleName() + " is annotated with @ConfigurationProperties and @Component"
				+ ". This may cause the @ConfigurationProperties bean to be registered twice.";
	}

	private String getBasicAction(Class<?> target) {
		return "Remove @Component from " + target.getName()
				+ " or consider disabling automatic @ConfigurationProperties scanning.";
	}

	private FailureAnalysis performAnalysis(Class<?> target) {
		FailureAnalysis analysis = this.analyzer
				.analyze(new InvalidConfigurationPropertiesException(target, Component.class));
		assertThat(analysis).isNotNull();
		return analysis;
	}

	@ConfigurationProperties
	@Component
	static class TestProperties {

	}

	@ConfigurationProperties
	@Component
	static class TestPropertiesWithSingleConstructor {

		TestPropertiesWithSingleConstructor(Object firstService, Function<String, Integer> factory) {

		}

	}

	@ConfigurationProperties
	@Component
	static class TestPropertiesWithSeveralConstructors {

		TestPropertiesWithSeveralConstructors() {
		}

		@Autowired
		TestPropertiesWithSeveralConstructors(Object someService) {

		}

	}

}
