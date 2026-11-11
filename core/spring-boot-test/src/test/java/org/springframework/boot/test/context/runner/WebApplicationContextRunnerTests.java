/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.runner;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebApplicationContextRunner}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class WebApplicationContextRunnerTests extends
		AbstractApplicationContextRunnerTests<WebApplicationContextRunner, ConfigurableWebApplicationContext, AssertableWebApplicationContext> {

	@Test
	void contextShouldHaveMockServletContext() {
		get().run((context) -> assertThat(context.getServletContext()).isInstanceOf(MockServletContext.class));
	}

	@Override
	protected WebApplicationContextRunner get() {
		return new WebApplicationContextRunner();
	}

	@Override
	protected WebApplicationContextRunner getWithAdditionalContextInterface() {
		return new WebApplicationContextRunner(TestAnnotationConfigServletWebApplicationContext::new,
				AdditionalContextInterface.class);
	}

	static class TestAnnotationConfigServletWebApplicationContext extends AnnotationConfigServletWebApplicationContext
			implements AdditionalContextInterface {

	}

}
