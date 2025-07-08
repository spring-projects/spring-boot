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

package org.springframework.boot.webflux.autoconfigure.actuate.web;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.actuate.mappings.DispatcherHandlersMappingDescriptionProvider;
import org.springframework.web.reactive.DispatcherHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxMappingsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class WebFluxMappingsAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebFluxMappingsAutoConfiguration.class));

	@Test
	void whenEndpointIsUnavailableThenDescriptionProviderIsNotCreated() {
		this.contextRunner.withBean(DispatcherHandler.class)
			.run((context) -> assertThat(context).doesNotHaveBean(DispatcherHandlersMappingDescriptionProvider.class));
	}

	@Test
	void whenEndpointIsAvailableButThereIsNoDispatcherHandlerThenDescriptionProviderIsNotCreated() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=mappings")
			.run((context) -> assertThat(context).doesNotHaveBean(DispatcherHandlersMappingDescriptionProvider.class));
	}

	@Test
	void whenEndpointIsAvailableThenDescriptionProviderIsCreated() {
		this.contextRunner.withBean(DispatcherHandler.class)
			.withPropertyValues("management.endpoints.web.exposure.include=mappings")
			.run((context) -> assertThat(context).hasSingleBean(DispatcherHandlersMappingDescriptionProvider.class));
	}

}
