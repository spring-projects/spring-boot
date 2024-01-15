/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.sbom;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.sbom.SbomEndpoint;
import org.springframework.boot.actuate.sbom.SbomEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SbomEndpointAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class SbomEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SbomEndpointAutoConfiguration.class));

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=sbom")
			.run((context) -> assertThat(context).hasSingleBean(SbomEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(SbomEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.sbom.enabled:false")
			.withPropertyValues("management.endpoints.web.exposure.include=*")
			.run((context) -> assertThat(context).doesNotHaveBean(SbomEndpoint.class));
	}

	@Test
	void runWhenOnlyExposedOverJmxShouldHaveEndpointBeanWithoutWebExtension() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=info", "spring.jmx.enabled=true",
					"management.endpoints.jmx.exposure.include=sbom")
			.run((context) -> assertThat(context).hasSingleBean(SbomEndpoint.class)
				.doesNotHaveBean(SbomEndpointWebExtension.class));
	}

}
