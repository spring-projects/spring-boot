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

package org.springframework.boot.actuate.autoconfigure.startup;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.startup.StartupEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StartupEndpointAutoConfiguration}
 *
 * @author Brian Clozel
 */
class StartupEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(StartupEndpointAutoConfiguration.class));

	@Test
	void runShouldNotHaveStartupEndpoint() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(StartupEndpoint.class));
	}

	@Test
	void runWhenMissingAppStartupShouldNotHaveStartupEndpoint() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=startup")
				.run((context) -> assertThat(context).doesNotHaveBean(StartupEndpoint.class));
	}

	@Test
	void runShouldHaveStartupEndpoint() {
		new ApplicationContextRunner(() -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setApplicationStartup(new BufferingApplicationStartup(1));
			return context;
		}).withConfiguration(AutoConfigurations.of(StartupEndpointAutoConfiguration.class))
				.withPropertyValues("management.endpoints.web.exposure.include=startup")
				.run((context) -> assertThat(context).hasSingleBean(StartupEndpoint.class));
	}

}
