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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointConfiguration.HealthEndpointGroupMembershipValidator.NoSuchHealthContributorException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchHealthContributorFailureAnalyzer}.
 *
 * @author Moritz Halbritter
 */
class NoSuchHealthContributorFailureAnalyzerTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HealthEndpointAutoConfiguration.class));

	@Test
	void analyzesMissingRequiredConfiguration() throws Throwable {
		FailureAnalysis analysis = new NoSuchHealthContributorFailureAnalyzer().analyze(createFailure());
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
			.isEqualTo("Included health contributor 'dummy' in group 'readiness' does not exist");
		assertThat(analysis.getAction()).isEqualTo("Update your application to correct the invalid configuration.\n"
				+ "You can also set 'management.endpoint.health.validate-group-membership' to false to disable the validation.");
	}

	private Throwable createFailure() throws Throwable {
		AtomicReference<Throwable> failure = new AtomicReference<>();
		this.runner.withPropertyValues("management.endpoint.health.group.readiness.include=dummy").run((context) -> {
			assertThat(context).hasFailed();
			failure.set(context.getStartupFailure());
		});
		Throwable throwable = failure.get();
		if (throwable instanceof NoSuchHealthContributorException) {
			return throwable;
		}
		throw throwable;
	}

}
