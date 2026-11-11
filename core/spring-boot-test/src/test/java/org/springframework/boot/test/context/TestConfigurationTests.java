/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestConfiguration @TestConfiguration}.
 *
 * @author Stephane Nicoll
 */
class TestConfigurationTests {

	@Test
	void proxyBeanMethodsIsEnabledByDefault() {
		AnnotationAttributes attributes = AnnotatedElementUtils
			.getMergedAnnotationAttributes(DefaultTestConfiguration.class, Configuration.class);
		assertThat(attributes).containsEntry("proxyBeanMethods", true);
	}

	@Test
	void proxyBeanMethodsCanBeDisabled() {
		AnnotationAttributes attributes = AnnotatedElementUtils
			.getMergedAnnotationAttributes(NoBeanMethodProxyingTestConfiguration.class, Configuration.class);
		assertThat(attributes).containsEntry("proxyBeanMethods", false);
	}

	@TestConfiguration
	static class DefaultTestConfiguration {

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class NoBeanMethodProxyingTestConfiguration {

	}

}
