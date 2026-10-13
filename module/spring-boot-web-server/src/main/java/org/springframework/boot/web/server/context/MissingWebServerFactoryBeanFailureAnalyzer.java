/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import java.util.Locale;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by a
 * {@link MissingWebServerFactoryBeanException}.
 *
 * @author Guirong Hu
 * @author Andy Wilkinson
 */
@Order(0)
class MissingWebServerFactoryBeanFailureAnalyzer extends AbstractFailureAnalyzer<MissingWebServerFactoryBeanException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MissingWebServerFactoryBeanException cause) {
		Class<?> beanType = cause.getBeanType();
		Assert.state(beanType != null, "'beanType' must not be null");
		return new FailureAnalysis(
				"Web application could not be started as there was no " + beanType.getName()
						+ " bean defined in the context.",
				"Check your application's dependencies for a supported "
						+ cause.getWebApplicationType().name().toLowerCase(Locale.ENGLISH) + " web server.\n"
						+ "Check the configured web application type.",
				cause);
	}

}
