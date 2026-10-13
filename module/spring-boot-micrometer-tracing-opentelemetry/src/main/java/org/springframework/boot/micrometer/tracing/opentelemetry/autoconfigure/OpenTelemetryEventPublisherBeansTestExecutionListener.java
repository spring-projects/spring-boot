/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * JUnit {@link TestExecutionListener} to ensure
 * {@link OpenTelemetryEventPublisherBeansApplicationListener#addWrapper()} is called as
 * early as possible.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see OpenTelemetryEventPublisherBeansApplicationListener
 */
public class OpenTelemetryEventPublisherBeansTestExecutionListener implements TestExecutionListener {

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		OpenTelemetryEventPublisherBeansApplicationListener.addWrapper();
	}

}
