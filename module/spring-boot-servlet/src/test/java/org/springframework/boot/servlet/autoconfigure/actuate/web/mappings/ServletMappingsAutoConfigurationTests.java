/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.autoconfigure.actuate.web.mappings;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.servlet.actuate.web.mappings.FiltersMappingDescriptionProvider;
import org.springframework.boot.servlet.actuate.web.mappings.ServletsMappingDescriptionProvider;
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
