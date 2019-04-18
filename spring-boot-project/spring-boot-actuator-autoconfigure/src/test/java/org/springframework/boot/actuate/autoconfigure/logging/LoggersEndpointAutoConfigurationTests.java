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

package org.springframework.boot.actuate.autoconfigure.logging;

import org.junit.Test;

import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LoggersEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class LoggersEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(LoggersEndpointAutoConfiguration.class))
			.withUserConfiguration(LoggingConfiguration.class);

	@Test
	public void runShouldHaveEndpointBean() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=loggers")
				.run((context) -> assertThat(context)
						.hasSingleBean(LoggersEndpoint.class));
	}

	@Test
	public void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.loggers.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(LoggersEndpoint.class));
	}

	@Test
	public void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run(
				(context) -> assertThat(context).doesNotHaveBean(LoggersEndpoint.class));
	}

	@Test
	public void runWithNoneLoggingSystemShouldNotHaveEndpointBean() {
		this.contextRunner
				.withSystemProperties(
						"org.springframework.boot.logging.LoggingSystem=none")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(LoggersEndpoint.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class LoggingConfiguration {

		@Bean
		public LoggingSystem loggingSystem() {
			return mock(LoggingSystem.class);
		}

	}

}
