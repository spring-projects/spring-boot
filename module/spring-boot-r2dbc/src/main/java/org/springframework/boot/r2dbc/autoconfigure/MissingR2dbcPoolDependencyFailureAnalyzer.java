/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * {@link FailureAnalyzer} for {@link MissingR2dbcPoolDependencyException}.
 *
 * @author Andy Wilkinson
 */
class MissingR2dbcPoolDependencyFailureAnalyzer extends AbstractFailureAnalyzer<MissingR2dbcPoolDependencyException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MissingR2dbcPoolDependencyException cause) {
		return new FailureAnalysis(cause.getMessage(),
				"Update your application's build to depend on io.r2dbc:r2dbc-pool or your application's configuration "
						+ "to disable R2DBC connection pooling.",
				cause);
	}

}
