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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.newrelic.NewRelicMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link MissingRequiredConfigurationFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class MissingRequiredConfigurationFailureAnalyzerTests {

	@Test
	void analyzesMissingRequiredConfiguration() {
		FailureAnalysis analysis = new MissingRequiredConfigurationFailureAnalyzer()
				.analyze(createFailure(MissingAccountIdConfiguration.class));
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).isEqualTo("accountId must be set to report metrics to New Relic.");
		assertThat(analysis.getAction()).isEqualTo("Update your application to provide the missing configuration.");
	}

	private Exception createFailure(Class<?> configuration) {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
			fail("Expected failure did not occur");
			return null;
		}
		catch (Exception ex) {
			return ex;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class MissingAccountIdConfiguration {

		@Bean
		public NewRelicMeterRegistry meterRegistry() {
			return new NewRelicMeterRegistry((key) -> null, Clock.SYSTEM);
		}

	}

}
