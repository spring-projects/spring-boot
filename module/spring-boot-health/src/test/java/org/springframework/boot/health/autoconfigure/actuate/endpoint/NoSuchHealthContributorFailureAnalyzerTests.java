/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointConfiguration.HealthEndpointGroupMembershipValidator.NoSuchHealthContributorException;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchHealthContributorFailureAnalyzer}.
 *
 * @author Moritz Halbritter
 */
class NoSuchHealthContributorFailureAnalyzerTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
		.of(HealthEndpointAutoConfiguration.class, HealthContributorRegistryAutoConfiguration.class));

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
			Throwable startupFailure = context.getStartupFailure();
			assertThat(startupFailure).isNotNull();
			failure.set(startupFailure);
		});
		Throwable throwable = failure.get();
		if (throwable instanceof NoSuchHealthContributorException) {
			return throwable;
		}
		throw throwable;
	}

}
