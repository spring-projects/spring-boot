/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.autoconfigure.actuate.web;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletManagementChildContextConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ServletManagementChildContextConfigurationTests {

	@Test // gh-45857
	void doesNotCreateManagementServerPropertiesInChildContext() {
		new WebApplicationContextRunner().withBean(ManagementServerProperties.class)
			.run((parent) -> new WebApplicationContextRunner().withParent(parent)
				.withUserConfiguration(ServletManagementChildContextConfiguration.class)
				.run((context) -> assertThat(context.getBean(ManagementServerProperties.class))
					.isSameAs(parent.getBean(ManagementServerProperties.class))));
	}

}
