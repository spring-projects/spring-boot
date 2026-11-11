/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code IncompatibleConfigurationException}.
 *
 * @author Brian Clozel
 */
class IncompatibleConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<IncompatibleConfigurationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, IncompatibleConfigurationException cause) {
		String action = String.format("Review the docs for %s and change the configured values.",
				String.join(", ", cause.getIncompatibleKeys()));
		return new FailureAnalysis(cause.getMessage(), action, cause);
	}

}
