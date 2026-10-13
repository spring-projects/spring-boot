/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.ssl;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of non-watchable bundle
 * content failures caused by {@link BundleContentNotWatchableException}.
 *
 * @author Moritz Halbritter
 */
class BundleContentNotWatchableFailureAnalyzer extends AbstractFailureAnalyzer<BundleContentNotWatchableException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, BundleContentNotWatchableException cause) {
		return new FailureAnalysis(cause.getMessage(), "Update your application to correct the invalid configuration:\n"
				+ "Either use a watchable resource, or disable bundle reloading by setting reload-on-update = false on the bundle.",
				cause);
	}

}
