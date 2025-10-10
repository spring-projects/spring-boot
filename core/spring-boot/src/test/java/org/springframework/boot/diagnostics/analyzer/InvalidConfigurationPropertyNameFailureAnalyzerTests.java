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

package org.springframework.boot.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InvalidConfigurationPropertyNameFailureAnalyzer}.
 *
 * @author Madhura Bhave
 */
class InvalidConfigurationPropertyNameFailureAnalyzerTests {

	private final InvalidConfigurationPropertyNameFailureAnalyzer analyzer = new InvalidConfigurationPropertyNameFailureAnalyzer();

	@Test
	void analysisWhenRootCauseIsBeanCreationFailureShouldContainBeanName() {
		BeanCreationException failure = createFailure(InvalidPrefixConfiguration.class);
		assertThat(failure).isNotNull();
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
			.contains(String.format("%n    Invalid characters: %s%n    Bean: %s%n    Reason: %s", "'F', 'P'",
					"invalidPrefixProperties", "Canonical names should be kebab-case ('-' separated), "
							+ "lowercase alpha-numeric characters and must start with a letter"));
	}

	private @Nullable BeanCreationException createFailure(Class<?> configuration) {
		try {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.register(configuration);
			context.refresh();
			context.close();
			return null;
		}
		catch (BeanCreationException ex) {
			return ex;
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(InvalidPrefixProperties.class)
	static class InvalidPrefixConfiguration {

		@Bean(name = "invalidPrefixProperties")
		InvalidPrefixProperties invalidPrefixProperties() {
			return new InvalidPrefixProperties();
		}

	}

	@ConfigurationProperties("FooPrefix")
	static class InvalidPrefixProperties {

		private @Nullable String value;

		@Nullable String getValue() {
			return this.value;
		}

		void setValue(@Nullable String value) {
			this.value = value;
		}

	}

}
