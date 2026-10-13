/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SemanticConventions}.
 *
 * @author Andy Wilkinson
 */
class SemanticConventionsTests {

	@Test
	void exactMatch() {
		MockEnvironment environment = new MockEnvironment();
		TestPropertyValues.of("management.observations.conventions=MICROMETER").applyTo(environment);
		assertThat(SemanticConventions.MICROMETER.isActive(environment)).isTrue();
		assertThat(SemanticConventions.OPEN_TELEMETRY.isActive(environment)).isFalse();
	}

	@Test
	void caseInsensitiveMatch() {
		MockEnvironment environment = new MockEnvironment();
		TestPropertyValues.of("management.observations.conventions=open_telemetry").applyTo(environment);
		assertThat(SemanticConventions.MICROMETER.isActive(environment)).isFalse();
		assertThat(SemanticConventions.OPEN_TELEMETRY.isActive(environment)).isTrue();
	}

	@Test
	void ignoresUnderscoreMatch() {
		MockEnvironment environment = new MockEnvironment();
		TestPropertyValues.of("management.observations.conventions=opentelemetry").applyTo(environment);
		assertThat(SemanticConventions.MICROMETER.isActive(environment)).isFalse();
		assertThat(SemanticConventions.OPEN_TELEMETRY.isActive(environment)).isTrue();
	}

}
