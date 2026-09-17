/*
 * Copyright 2012-present the original author or authors.
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
