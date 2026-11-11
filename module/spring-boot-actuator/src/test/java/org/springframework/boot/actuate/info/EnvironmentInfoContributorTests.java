/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.util.TestPropertyValues.Type;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentInfoContributor}.
 *
 * @author Stephane Nicoll
 */
class EnvironmentInfoContributorTests {

	private final StandardEnvironment environment = new StandardEnvironment();

	@Test
	void extractOnlyInfoProperty() {
		TestPropertyValues.of("info.app=my app", "info.version=1.0.0", "foo=bar").applyTo(this.environment);
		Info actual = contributeFrom(this.environment);
		assertThat(actual.get("app", String.class)).isEqualTo("my app");
		assertThat(actual.get("version", String.class)).isEqualTo("1.0.0");
		assertThat(actual.getDetails()).hasSize(2);
	}

	@Test
	void extractNoEntry() {
		TestPropertyValues.of("foo=bar").applyTo(this.environment);
		Info actual = contributeFrom(this.environment);
		assertThat(actual.getDetails()).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void propertiesFromEnvironmentShouldBindCorrectly() {
		TestPropertyValues.of("INFO_ENVIRONMENT_FOO=green").applyTo(this.environment, Type.SYSTEM_ENVIRONMENT);
		Info actual = contributeFrom(this.environment);
		assertThat(actual.get("environment", Map.class)).containsEntry("foo", "green");
	}

	private static Info contributeFrom(ConfigurableEnvironment environment) {
		EnvironmentInfoContributor contributor = new EnvironmentInfoContributor(environment);
		Info.Builder builder = new Info.Builder();
		contributor.contribute(builder);
		return builder.build();
	}

}
