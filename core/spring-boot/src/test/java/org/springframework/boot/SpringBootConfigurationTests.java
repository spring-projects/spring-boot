/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootConfiguration @SpringBootConfiguration}.
 *
 * @author Andy Wilkinson
 */
class SpringBootConfigurationTests {

	@Test
	void proxyBeanMethodsIsEnabledByDefault() {
		AnnotationAttributes attributes = AnnotatedElementUtils
			.getMergedAnnotationAttributes(DefaultSpringBootConfiguration.class, Configuration.class);
		assertThat(attributes).containsEntry("proxyBeanMethods", true);
	}

	@Test
	void proxyBeanMethodsCanBeDisabled() {
		AnnotationAttributes attributes = AnnotatedElementUtils
			.getMergedAnnotationAttributes(NoBeanMethodProxyingSpringBootConfiguration.class, Configuration.class);
		assertThat(attributes).containsEntry("proxyBeanMethods", false);
	}

	@SpringBootConfiguration
	static class DefaultSpringBootConfiguration {

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	static class NoBeanMethodProxyingSpringBootConfiguration {

	}

}
