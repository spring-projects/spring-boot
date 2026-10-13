/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.mappings;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappingsEndpointAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class MappingsEndpointAutoConfigurationTests {

	@Test
	void whenEndpointIsUnavailableThenEndpointIsNotCreated() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MappingsEndpointAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(MappingsEndpoint.class));
	}

	@Test
	void whenEndpointIsAvailableThenEndpointIsCreated() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MappingsEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include=mappings")
			.run((context) -> assertThat(context).hasSingleBean(MappingsEndpoint.class));
	}

}
