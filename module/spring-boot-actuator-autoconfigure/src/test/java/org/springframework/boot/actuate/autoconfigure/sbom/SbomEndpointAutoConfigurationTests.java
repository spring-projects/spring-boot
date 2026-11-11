/*
 * Copyright 2012-present the original author or authors.
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
	void runWhenWebExposedShouldHaveEndpointBeanAndWebExtension() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=sbom")
			.run((context) -> assertThat(context).hasSingleBean(SbomEndpoint.class)
				.hasSingleBean(SbomEndpointWebExtension.class));
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
