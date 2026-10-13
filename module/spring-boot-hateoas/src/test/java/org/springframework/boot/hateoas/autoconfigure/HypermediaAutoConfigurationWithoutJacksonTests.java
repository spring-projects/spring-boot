/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hateoas.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.mock.web.MockServletContext;

/**
 * Tests for {@link HypermediaAutoConfiguration} when Jackson is not on the classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-*.jar")
class HypermediaAutoConfigurationWithoutJacksonTests {

	private @Nullable AnnotationConfigServletWebApplicationContext context;

	@Test
	void jacksonRelatedConfigurationBacksOff() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(BaseConfig.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
	}

	@ImportAutoConfiguration(HypermediaAutoConfiguration.class)
	static class BaseConfig {

	}

}
