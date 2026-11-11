/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure.actuate.web;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxManagementChildContextConfiguration}.
 *
 * @author Moritz Halbritter
 */
class WebFluxManagementChildContextConfigurationTests {

	@Test
	// gh-45857
	void failsWithoutManagementServerPropertiesBeanFromParent() {
		new ReactiveWebApplicationContextRunner()
			.run((parent) -> new ReactiveWebApplicationContextRunner().withParent(parent)
				.withUserConfiguration(WebFluxManagementChildContextConfiguration.class)
				.run((context) -> assertThat(context).hasFailed()));
	}

	@Test
	// gh-45857
	void succeedsWithManagementServerPropertiesBeanFromParent() {
		new ReactiveWebApplicationContextRunner().withBean(ManagementServerProperties.class)
			.run((parent) -> new ReactiveWebApplicationContextRunner().withParent(parent)
				.withUserConfiguration(WebFluxManagementChildContextConfiguration.class)
				.run((context) -> assertThat(context).hasNotFailed()));
	}

}
