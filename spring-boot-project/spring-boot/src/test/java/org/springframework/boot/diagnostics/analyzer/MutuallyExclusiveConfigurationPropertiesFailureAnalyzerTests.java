/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MutuallyExclusiveConfigurationPropertiesFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class MutuallyExclusiveConfigurationPropertiesFailureAnalyzerTests {

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	void analyzeWhenEnvironmentIsNullShouldReturnNull() {
		MutuallyExclusiveConfigurationPropertiesException failure = new MutuallyExclusiveConfigurationPropertiesException(
				new HashSet<>(Arrays.asList("com.example.a", "com.example.b")),
				new HashSet<>(Arrays.asList("com.example.a", "com.example.b")));
		FailureAnalysis failureAnalysis = new MutuallyExclusiveConfigurationPropertiesFailureAnalyzer()
				.analyze(failure);
		assertThat(failureAnalysis).isNull();
	}

	@Test
	void analyzeWhenNotAllPropertiesAreInTheEnvironmentShouldReturnNull() {
		MapPropertySource source = new MapPropertySource("test", Collections.singletonMap("com.example.a", "alpha"));
		this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
		MutuallyExclusiveConfigurationPropertiesException failure = new MutuallyExclusiveConfigurationPropertiesException(
				new HashSet<>(Arrays.asList("com.example.a", "com.example.b")),
				new HashSet<>(Arrays.asList("com.example.a", "com.example.b")));
		FailureAnalysis analysis = performAnalysis(failure);
		assertThat(analysis).isNull();
	}

	@Test
	void analyzeWhenAllConfiguredPropertiesAreInTheEnvironmentShouldReturnAnalysis() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("com.example.a", "alpha");
		properties.put("com.example.b", "bravo");
		MapPropertySource source = new MapPropertySource("test", properties);
		this.environment.getPropertySources().addFirst(OriginCapablePropertySource.get(source));
		MutuallyExclusiveConfigurationPropertiesException failure = new MutuallyExclusiveConfigurationPropertiesException(
				new HashSet<>(Arrays.asList("com.example.a", "com.example.b")),
				new HashSet<>(Arrays.asList("com.example.a", "com.example.b")));
		FailureAnalysis analysis = performAnalysis(failure);
		assertThat(analysis.getAction()).isEqualTo(
				"Update your configuration so that only one of the mutually exclusive properties is configured.");
		assertThat(analysis.getDescription()).contains(String.format(
				"The following configuration properties are mutually exclusive:%n%n\tcom.example.a%n\tcom.example.b%n"))
				.contains(String
						.format("However, more than one of those properties has been configured at the same time:%n%n"
								+ "\tcom.example.a (originating from 'TestOrigin test')%n"
								+ "\tcom.example.b (originating from 'TestOrigin test')%n"));
	}

	@Test
	void analyzeWhenPropertyIsInMultiplePropertySourcesShouldListEachSourceInAnalysis() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("com.example.a", "alpha");
		properties.put("com.example.b", "bravo");
		this.environment.getPropertySources()
				.addFirst(OriginCapablePropertySource.get(new MapPropertySource("test-one", properties)));
		this.environment.getPropertySources()
				.addLast(OriginCapablePropertySource.get(new MapPropertySource("test-two", properties)));
		MutuallyExclusiveConfigurationPropertiesException failure = new MutuallyExclusiveConfigurationPropertiesException(
				new HashSet<>(Arrays.asList("com.example.a", "com.example.b")),
				new HashSet<>(Arrays.asList("com.example.a", "com.example.b")));
		FailureAnalysis analysis = performAnalysis(failure);
		assertThat(analysis.getAction()).isEqualTo(
				"Update your configuration so that only one of the mutually exclusive properties is configured.");
		assertThat(analysis.getDescription()).contains(String.format(
				"The following configuration properties are mutually exclusive:%n%n\tcom.example.a%n\tcom.example.b%n"))
				.contains(String
						.format("However, more than one of those properties has been configured at the same time:%n%n"
								+ "\tcom.example.a (originating from 'TestOrigin test-one')%n"
								+ "\tcom.example.a (originating from 'TestOrigin test-two')%n"
								+ "\tcom.example.b (originating from 'TestOrigin test-one')%n"
								+ "\tcom.example.b (originating from 'TestOrigin test-two')%n"));
	}

	private FailureAnalysis performAnalysis(MutuallyExclusiveConfigurationPropertiesException failure) {
		MutuallyExclusiveConfigurationPropertiesFailureAnalyzer analyzer = new MutuallyExclusiveConfigurationPropertiesFailureAnalyzer();
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
					return "TestOrigin " + getName();
				}

			};
		}

		static <T> OriginCapablePropertySource<T> get(EnumerablePropertySource<T> propertySource) {
			return new OriginCapablePropertySource<>(propertySource);
		}

	}

}
