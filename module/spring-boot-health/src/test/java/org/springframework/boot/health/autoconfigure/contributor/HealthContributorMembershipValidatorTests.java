/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalyzedException;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.DefaultReactiveHealthContributorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link HealthContributorMembershipValidator}.
 *
 * @author Phillip Webb
 */
class HealthContributorMembershipValidatorTests {

	private static final HealthIndicator UP = () -> Health.up().build();

	private final DefaultHealthContributorRegistry registry = new DefaultHealthContributorRegistry();

	private final DefaultReactiveHealthContributorRegistry fallbackRegistry = new DefaultReactiveHealthContributorRegistry();

	@Test
	void validateWhenNoNamesDoesNotThrow() {
		HealthContributorMembershipValidator validator = new HealthContributorMembershipValidator(this.registry, null,
				"my.disable-validation", (members) -> members.member("include", Collections.emptySet()));
		validator.afterSingletonsInstantiated();
	}

	@Test
	void validateWhenNameIncludesAsteriskDoesNotThrow() {
		HealthContributorMembershipValidator validator = new HealthContributorMembershipValidator(this.registry, null,
				"my.disable-validation", (members) -> members.member("include", Set.of("*")));
		validator.afterSingletonsInstantiated();
	}

	@Test
	void validateWhenContributorDoesNotExistThrows() {
		HealthContributorMembershipValidator validator = new HealthContributorMembershipValidator(this.registry, null,
				"my.disable-validation", (members) -> members.member("include", Set.of("test")));
		assertThatExceptionOfType(FailureAnalyzedException.class)
			.isThrownBy(() -> validator.afterSingletonsInstantiated())
			.withMessage("Health contributor 'test' defined in 'include' does not exist")
			.satisfies((ex) -> assertThat(ex.analysis().getAction()).contains("my.disable-validation"));
	}

	@Test
	void validateWhenContributorExistsInRegistryDoesNotThrow() {
		this.registry.registerContributor("test", UP);
		HealthContributorMembershipValidator validator = new HealthContributorMembershipValidator(this.registry, null,
				"my.disable-validation", (members) -> members.member("include", Set.of("test")));
		validator.afterSingletonsInstantiated();
	}

	@Test
	void validateWhenContributorExistsInFallbackRegistryDoesNotThrow() {
		this.fallbackRegistry.registerContributor("test", ReactiveHealthContributor.adapt(UP));
		HealthContributorMembershipValidator validator = new HealthContributorMembershipValidator(this.registry,
				this.fallbackRegistry, "my.disable-validation", (members) -> members.member("include", Set.of("test")));
		validator.afterSingletonsInstantiated();
	}

	@Test
	void validateWhenNestedContributorExistsInRegistryDoesNotThrow() {
		this.registry.registerContributor("test", CompositeHealthContributor.fromMap(Map.of("nested", UP)));
		HealthContributorMembershipValidator validator = new HealthContributorMembershipValidator(this.registry, null,
				"my.disable-validation", (members) -> members.member("include", Set.of("test/nested")));
		validator.afterSingletonsInstantiated();
	}

}
