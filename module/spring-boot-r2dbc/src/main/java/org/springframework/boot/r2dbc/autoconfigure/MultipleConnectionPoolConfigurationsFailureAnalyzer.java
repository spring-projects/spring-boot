/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * {@link FailureAnalyzer} for {@link MultipleConnectionPoolConfigurationsException}.
 *
 * @author Andy Wilkinson
 */
class MultipleConnectionPoolConfigurationsFailureAnalyzer
		extends AbstractFailureAnalyzer<MultipleConnectionPoolConfigurationsException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MultipleConnectionPoolConfigurationsException cause) {
		return new FailureAnalysis(cause.getMessage(),
				"Update your configuration so that R2DBC connection pooling is configured using either the "
						+ "spring.r2dbc.url property or the spring.r2dbc.pool.* properties",
				cause);
	}

}
