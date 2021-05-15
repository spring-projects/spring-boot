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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InvalidConfigurationPropertyValueFailureAnalyzer}.
 *
 * @author Stephane Nicoll
 */
class InvalidConfigurationPropertyValueFailureAnalyzerTests {

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	void analysisWithNullEnvironment() {
		InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
				"test.property", "invalid", "This is not valid.");
		FailureAnalysis analysis = new InvalidConfigurationPropertyValueFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNull();
	}

	@Test
	void analysisWithKnownProperty() {
		MapPropertySource source = new MapPropertySource("test", Collections.singletonMap("test.property", "invalid"));
		this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
		InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
				"test.property", "invalid", "This is not valid.");
		FailureAnalysis analysis = performAnalysis(failure);
		assertCommonParts(failure, analysis);
		assertThat(analysis.getAction()).contains("Review the value of the property with the provided reason.");
		assertThat(analysis.getDescription()).contains("Validation failed for the following reason")
				.contains("This is not valid.").doesNotContain("Additionally, this property is also set");
	}

	@Test
	void analysisWithKnownPropertyAndNoReason() {
		MapPropertySource source = new MapPropertySource("test", Collections.singletonMap("test.property", "invalid"));
		this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
		InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
				"test.property", "invalid", null);
		FailureAnalysis analysis = performAnalysis(failure);
		assertThat(analysis.getAction()).contains("Review the value of the property.");
		assertThat(analysis.getDescription()).contains("No reason was provided.")
				.doesNotContain("Additionally, this property is also set");
	}

	@Test
	void analysisWithKnownPropertyAndOtherCandidates() {
		MapPropertySource source = new MapPropertySource("test", Collections.singletonMap("test.property", "invalid"));
		MapPropertySource additional = new MapPropertySource("additional",
				Collections.singletonMap("test.property", "valid"));
		MapPropertySource another = new MapPropertySource("another", Collections.singletonMap("test.property", "test"));
		this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
		this.environment.getPropertySources().addLast(additional);
		this.environment.getPropertySources().addLast(OriginCapablePropertySource.get(another));
		InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
				"test.property", "invalid", "This is not valid.");
		FailureAnalysis analysis = performAnalysis(failure);
		assertCommonParts(failure, analysis);
		assertThat(analysis.getAction()).contains("Review the value of the property with the provided reason.");
		assertThat(analysis.getDescription())
				.contains("Additionally, this property is also set in the following property sources:")
				.contains("In 'additional' with the value 'valid'")
				.contains("In 'another' with the value 'test' (originating from 'TestOrigin test.property')");
	}

	@Test
	void analysisWithUnknownKey() {
		InvalidConfigurationPropertyValueException failure = new InvalidConfigurationPropertyValueException(
				"test.key.not.defined", "invalid", "This is not valid.");
		assertThat(performAnalysis(failure)).isNull();
	}

	private void assertCommonParts(InvalidConfigurationPropertyValueException failure, FailureAnalysis analysis) {
		assertThat(analysis.getDescription()).contains("test.property").contains("invalid")
				.contains("TestOrigin test.property");
		assertThat(analysis.getCause()).isSameAs(failure);
	}

	private FailureAnalysis performAnalysis(InvalidConfigurationPropertyValueException failure) {
		InvalidConfigurationPropertyValueFailureAnalyzer analyzer = new InvalidConfigurationPropertyValueFailureAnalyzer();
		analyzer.setEnvironment(this.environment);
		return analyzer.analyze(failure);
	}

	static class OriginCapablePropertySource<T> extends EnumerablePropertySource<T> implements OriginLookup<String> {

		private final EnumerablePropertySource<T> propertySource;

		OriginCapablePropertySource(EnumerablePropertySource<T> propertySource) {
			super(propertySource.getName(), propertySource.getSource());
			this.propertySource = propertySource;
		}

		@Override
		public Object getProperty(String name) {
			return this.propertySource.getProperty(name);
		}

		@Override
		public String[] getPropertyNames() {
			return this.propertySource.getPropertyNames();
		}

		@Override
		public Origin getOrigin(String name) {
			return new Origin() {

				@Override
				public String toString() {
					return "TestOrigin " + name;
				}

			};
		}

		static <T> OriginCapablePropertySource<T> get(EnumerablePropertySource<T> propertySource) {
			return new OriginCapablePropertySource<>(propertySource);
		}

	}

}
