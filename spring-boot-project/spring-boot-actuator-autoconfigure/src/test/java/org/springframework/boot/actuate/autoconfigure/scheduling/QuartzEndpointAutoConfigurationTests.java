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

package org.springframework.boot.actuate.autoconfigure.scheduling;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link QuartzEndpointAutoConfiguration}.
 *
 * @author Jordan Couret
 */
class QuartzEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(QuartzAutoConfiguration.class, QuartzEndpointAutoConfiguration.class));

	@Test
	void endpointQuartzIsAutoConfigured() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=quartz")
				.run((context) -> assertThat(context).hasSingleBean(QuartzEndpoint.class));
	}

	@Test
	void endpointQuartzNotAutoConfiguredWhenNotExposed() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

	@Test
	void endpointQuartzCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.endpoint.quartz.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

}
