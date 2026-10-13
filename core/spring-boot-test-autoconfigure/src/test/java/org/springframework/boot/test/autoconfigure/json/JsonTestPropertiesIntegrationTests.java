/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link JsonTest#properties properties} attribute of
 * {@link JsonTest @JsonTest}.
 *
 * @author Artsiom Yudovin
 */
@JsonTest(properties = "spring.profiles.active=test")
class JsonTestPropertiesIntegrationTests {

	@Autowired
	private Environment environment;

	@Test
	void environmentWithNewProfile() {
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
	}

	@Nested
	class NestedTests {

		@Autowired
		private Environment innerEnvironment;

		@Test
		void propertiesFromEnclosingClassAffectNestedTests() {
			assertThat(JsonTestPropertiesIntegrationTests.this.environment.getActiveProfiles()).containsExactly("test");
			assertThat(this.innerEnvironment.getActiveProfiles()).containsExactly("test");
		}

	}

}
