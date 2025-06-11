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

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.actuate.mappings.DispatcherServletsMappingDescriptionProvider;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcMappingsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class WebMvcMappingsAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebMvcMappingsAutoConfiguration.class));

	@Test
	void whenEndpointIsUnavailableThenDescriptionProviderIsNotCreated() {
		this.contextRunner.withBean(DispatcherServlet.class)
			.run((context) -> assertThat(context).doesNotHaveBean(DispatcherServletsMappingDescriptionProvider.class));
	}

	@Test
	void whenEndpointIsAvailableButThereIsNoDispatcherServletThenDescriptionProviderIsNotCreated() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=mappings")
			.run((context) -> assertThat(context).doesNotHaveBean(DispatcherServletsMappingDescriptionProvider.class));
	}

	@Test
	void whenEndpointIsAvailableThenDescriptionProviderIsCreated() {
		this.contextRunner.withBean(DispatcherServlet.class)
			.withPropertyValues("management.endpoints.web.exposure.include=mappings")
			.run((context) -> assertThat(context).hasSingleBean(DispatcherServletsMappingDescriptionProvider.class));
	}

}
