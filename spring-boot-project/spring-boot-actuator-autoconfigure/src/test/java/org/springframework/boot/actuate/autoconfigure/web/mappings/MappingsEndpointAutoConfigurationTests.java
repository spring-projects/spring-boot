/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.mappings;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappingsEndpointAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class MappingsEndpointAutoConfigurationTests {

	@Test
	void whenEndpointIsUnavailableThenEndpointAndDescriptionProvidersAreNotCreated() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MappingsEndpointAutoConfiguration.class,
						JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
						WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
						EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
						WebMvcEndpointManagementContextConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).doesNotHaveBean(MappingsEndpoint.class);
					assertThat(context).doesNotHaveBean(MappingDescriptionProvider.class);
				});

	}

	@Test
	void whenEndpointIsAvailableThenEndpointAndDescriptionProvidersAreCreated() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MappingsEndpointAutoConfiguration.class,
						JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
						WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
						EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
						WebMvcEndpointManagementContextConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
				.withPropertyValues("management.endpoints.web.exposure.include=mappings").run((context) -> {
					assertThat(context).hasSingleBean(MappingsEndpoint.class);
					assertThat(context.getBeansOfType(MappingDescriptionProvider.class)).hasSize(3);
				});

	}

}
