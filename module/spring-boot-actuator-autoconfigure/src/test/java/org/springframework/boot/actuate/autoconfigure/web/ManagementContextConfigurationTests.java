/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementContextConfiguration @ManagementContextConfiguration}.
 *
 * @author Andy Wilkinson
 */
class ManagementContextConfigurationTests {

	@Test
	void proxyBeanMethodsIsEnabledByDefault() {
		AnnotationAttributes attributes = AnnotatedElementUtils
			.getMergedAnnotationAttributes(DefaultManagementContextConfiguration.class, Configuration.class);
		assertThat(attributes).containsEntry("proxyBeanMethods", true);
	}

	@Test
	void proxyBeanMethodsCanBeDisabled() {
		AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(
				NoBeanMethodProxyingManagementContextConfiguration.class, Configuration.class);
		assertThat(attributes).containsEntry("proxyBeanMethods", false);
	}

	@ManagementContextConfiguration
	static class DefaultManagementContextConfiguration {

	}

	@ManagementContextConfiguration(proxyBeanMethods = false)
	static class NoBeanMethodProxyingManagementContextConfiguration {

	}

}
