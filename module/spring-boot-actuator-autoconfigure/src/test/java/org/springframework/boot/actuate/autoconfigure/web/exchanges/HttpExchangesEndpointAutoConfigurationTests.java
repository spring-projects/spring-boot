/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.exchanges;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.web.exchanges.HttpExchangesEndpoint;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpExchangesEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class HttpExchangesEndpointAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpExchangesEndpointAutoConfiguration.class));

	@Test
	void runWhenRepositoryBeanAvailableShouldHaveEndpointBean() {
		this.contextRunner.withUserConfiguration(HttpExchangeRepositoryConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=httpexchanges")
			.run((context) -> assertThat(context).hasSingleBean(HttpExchangesEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.withUserConfiguration(HttpExchangeRepositoryConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(HttpExchangesEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withUserConfiguration(HttpExchangeRepositoryConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=httpexchanges")
			.withPropertyValues("management.endpoint.httpexchanges.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(HttpExchangesEndpoint.class));
	}

	@Test
	void endpointBacksOffWhenRepositoryIsNotAvailable() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=httpexchanges")
			.run((context) -> assertThat(context).doesNotHaveBean(HttpExchangesEndpoint.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class HttpExchangeRepositoryConfiguration {

		@Bean
		InMemoryHttpExchangeRepository customHttpExchangeRepository() {
			return new InMemoryHttpExchangeRepository();
		}

	}

}
