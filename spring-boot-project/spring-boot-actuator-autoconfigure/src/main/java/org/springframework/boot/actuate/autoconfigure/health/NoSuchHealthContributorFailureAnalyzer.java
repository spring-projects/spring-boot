/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.health;

import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointConfiguration.HealthEndpointGroupMembershipValidator.NoSuchHealthContributorException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

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
