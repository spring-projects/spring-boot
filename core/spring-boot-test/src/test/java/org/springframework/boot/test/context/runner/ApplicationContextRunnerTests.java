/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.runner;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Tests for {@link ApplicationContextRunner}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ApplicationContextRunnerTests extends
		AbstractApplicationContextRunnerTests<ApplicationContextRunner, ConfigurableApplicationContext, AssertableApplicationContext> {

	@Override
	protected ApplicationContextRunner get() {
		return new ApplicationContextRunner();
	}

	@Override
	protected ApplicationContextRunner getWithAdditionalContextInterface() {
		return new ApplicationContextRunner(TestAnnotationConfigApplicationContext::new,
				AdditionalContextInterface.class);
	}

	static class TestAnnotationConfigApplicationContext extends AnnotationConfigApplicationContext
			implements AdditionalContextInterface {

	}

}
