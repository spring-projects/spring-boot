/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure.actuate.web.mappings;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.actuate.web.mappings.DispatcherHandlersMappingDescriptionProvider;
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
