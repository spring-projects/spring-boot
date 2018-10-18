/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4J2MetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class Log4J2MetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple()).withConfiguration(
					AutoConfigurations.of(Log4J2MetricsAutoConfiguration.class));

	@Test
	public void autoConfiguresLog4J2Metrics() {
		this.contextRunner
				.run((context) -> assertThat(context).hasSingleBean(Log4j2Metrics.class));
	}

	@Test
	public void allowsCustomLog4J2MetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomLog4J2MetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Log4j2Metrics.class)
						.hasBean("customLog4J2Metrics"));
	}

	@Configuration
	static class CustomLog4J2MetricsConfiguration {

		@Bean
		public Log4j2Metrics customLog4J2Metrics() {
			return new Log4j2Metrics();
		}

	}

}
