/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UnboundConfigurationPropertyFailureAnalyzer}.
 *
 * @author Madhura Bhave
 */
class UnboundConfigurationPropertyFailureAnalyzerTests {

	@BeforeEach
	void setup() {
		LocaleContextHolder.setLocale(Locale.US);
	}

	@AfterEach
	void cleanup() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void bindExceptionDueToUnboundElements() {
		FailureAnalysis analysis = performAnalysis(UnboundElementsFailureConfiguration.class,
				"test.foo.listValue[0]=hello", "test.foo.listValue[2]=world");
		assertThat(analysis.getDescription()).contains(
				failure("test.foo.listvalue[2]", "world", "\"test.foo.listValue[2]\" from property source \"test\"",
						"The elements [test.foo.listvalue[2]] were left unbound."));
	}

	private static String failure(String property, String value, String origin, String reason) {
		return String.format("Property: %s%n    Value: \"%s\"%n    Origin: %s%n    Reason: %s", property, value, origin,
				reason);
	}

	private FailureAnalysis performAnalysis(Class<?> configuration, String... environment) {
		BeanCreationException failure = createFailure(configuration, environment);
		assertThat(failure).isNotNull();
		return new UnboundConfigurationPropertyFailureAnalyzer().analyze(failure);
	}

	private BeanCreationException createFailure(Class<?> configuration, String... environment) {
		try {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			addEnvironment(context, environment);
			context.register(configuration);
			context.refresh();
			context.close();
			return null;
		}
		catch (BeanCreationException ex) {
			return ex;
		}
	}

	private void addEnvironment(AnnotationConfigApplicationContext context, String[] environment) {
		MutablePropertySources sources = context.getEnvironment().getPropertySources();
		Map<String, Object> map = new HashMap<>();
		for (String pair : environment) {
			int index = pair.indexOf('=');
			String key = (index > 0) ? pair.substring(0, index) : pair;
			String value = (index > 0) ? pair.substring(index + 1) : "";
			map.put(key.trim(), value.trim());
		}
		sources.addFirst(new MapPropertySource("test", map));
	}

	@EnableConfigurationProperties(UnboundElementsFailureProperties.class)
	static class UnboundElementsFailureConfiguration {

	}

	@ConfigurationProperties("test.foo")
	static class UnboundElementsFailureProperties {

		private List<String> listValue;

		List<String> getListValue() {
			return this.listValue;
		}

		void setListValue(List<String> listValue) {
			this.listValue = listValue;
		}

	}

}
