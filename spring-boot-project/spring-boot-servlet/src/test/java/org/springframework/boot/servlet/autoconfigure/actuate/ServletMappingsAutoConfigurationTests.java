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

package org.springframework.boot.servlet.autoconfigure.actuate;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.servlet.actuate.mappings.FiltersMappingDescriptionProvider;
import org.springframework.boot.servlet.actuate.mappings.ServletsMappingDescriptionProvider;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletMappingsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class ServletMappingsAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ServletMappingsAutoConfiguration.class));

	@Test
	void whenEndpointIsUnavailableThenDescriptionProvidersAreNotCreated() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(FiltersMappingDescriptionProvider.class)
			.doesNotHaveBean(ServletsMappingDescriptionProvider.class));
	}

	@Test
	void whenEndpointIsAvailableThenDescriptionProvidersAreCreated() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=mappings")
			.run((context) -> assertThat(context).hasSingleBean(FiltersMappingDescriptionProvider.class)
				.hasSingleBean(ServletsMappingDescriptionProvider.class));
	}

}
