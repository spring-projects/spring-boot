/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointConfiguration.HealthEndpointGroupMembershipValidator.NoSuchHealthContributorException;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link NoSuchHealthContributorException}.
 *
 * @author Moritz Halbritter
 */
class NoSuchHealthContributorFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchHealthContributorException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, NoSuchHealthContributorException cause) {
		return new FailureAnalysis(cause.getMessage(), "Update your application to correct the invalid configuration.\n"
				+ "You can also set 'management.endpoint.health.validate-group-membership' to false to disable the validation.",
				cause);
	}

}
