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

import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class LogbackMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple()).withConfiguration(
					AutoConfigurations.of(LogbackMetricsAutoConfiguration.class));

	@Test
	public void autoConfiguresLogbackMetrics() {
		this.contextRunner.run(
				(context) -> assertThat(context).hasSingleBean(LogbackMetrics.class));
	}

	@Test
	public void allowsCustomLogbackMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomLogbackMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(LogbackMetrics.class)
						.hasBean("customLogbackMetrics"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomLogbackMetricsConfiguration {

		@Bean
		public LogbackMetrics customLogbackMetrics() {
			return new LogbackMetrics();
		}

	}

}
