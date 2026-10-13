/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web.mappings;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.actuate.web.mappings.DispatcherServletsMappingDescriptionProvider;
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
