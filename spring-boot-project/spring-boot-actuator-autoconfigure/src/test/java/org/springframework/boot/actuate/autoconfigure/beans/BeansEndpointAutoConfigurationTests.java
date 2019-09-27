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

package org.springframework.boot.actuate.autoconfigure.beans;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeansEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class BeansEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(BeansEndpointAutoConfiguration.class));

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=beans")
				.run((context) -> assertThat(context).hasSingleBean(BeansEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(BeansEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.beans.enabled:false")
				.withPropertyValues("management.endpoints.web.exposure.include=*")
				.run((context) -> assertThat(context).doesNotHaveBean(BeansEndpoint.class));
	}

}
