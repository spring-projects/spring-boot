/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.runner;

import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.web.context.reactive.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.web.context.reactive.ConfigurableReactiveWebApplicationContext;

/**
 * Tests for {@link ReactiveWebApplicationContextRunner}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ReactiveWebApplicationContextRunnerTests extends
		AbstractApplicationContextRunnerTests<ReactiveWebApplicationContextRunner, ConfigurableReactiveWebApplicationContext, AssertableReactiveWebApplicationContext> {

	@Override
	protected ReactiveWebApplicationContextRunner get() {
		return new ReactiveWebApplicationContextRunner();
	}

	@Override
	protected ReactiveWebApplicationContextRunner getWithAdditionalContextInterface() {
		return new ReactiveWebApplicationContextRunner(TestAnnotationConfigReactiveWebApplicationContext::new,
				AdditionalContextInterface.class);
	}

	static class TestAnnotationConfigReactiveWebApplicationContext extends AnnotationConfigReactiveWebApplicationContext
			implements AdditionalContextInterface {

	}

}
