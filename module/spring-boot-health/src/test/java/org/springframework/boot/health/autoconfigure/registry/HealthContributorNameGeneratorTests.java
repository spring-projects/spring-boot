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

package org.springframework.boot.health.autoconfigure.registry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthContributorNameGenerator}.
 *
 * @author Phillip Webb
 */
class HealthContributorNameGeneratorTests {

	@Test
	void withoutStandardSuffixesWhenNameDoesNotEndWithSuffixReturnsName() {
		assertThat(HealthContributorNameGenerator.withoutStandardSuffixes().generateContributorName("test"))
			.isEqualTo("test");
	}

	@Test
	void withoutStandardSuffixesWhenNameEndsWithSuffixReturnsNewName() {
		assertThat(
				HealthContributorNameGenerator.withoutStandardSuffixes().generateContributorName("testHealthIndicator"))
			.isEqualTo("test");
		assertThat(HealthContributorNameGenerator.withoutStandardSuffixes()
			.generateContributorName("testHealthContributor")).isEqualTo("test");
	}

	@Test
	void withoutStandardSuffixesWhenNameEndsWithSuffixInDifferentReturnsNewName() {
		assertThat(
				HealthContributorNameGenerator.withoutStandardSuffixes().generateContributorName("testHEALTHindicator"))
			.isEqualTo("test");
		assertThat(HealthContributorNameGenerator.withoutStandardSuffixes()
			.generateContributorName("testHEALTHcontributor")).isEqualTo("test");
	}

	@Test
	void withoutStandardSuffixesWhenNameContainsSuffixReturnsName() {
		assertThat(HealthContributorNameGenerator.withoutStandardSuffixes()
			.generateContributorName("testHealthIndicatorTest")).isEqualTo("testHealthIndicatorTest");
		assertThat(HealthContributorNameGenerator.withoutStandardSuffixes()
			.generateContributorName("testHealthContributorTest")).isEqualTo("testHealthContributorTest");
	}

	@Test
	void withoutSuffixesStripsSuffix() {
		HealthContributorNameGenerator generator = HealthContributorNameGenerator.withoutSuffixes("spring", "boot");
		assertThat(generator.generateContributorName("testspring")).isEqualTo("test");
		assertThat(generator.generateContributorName("tEsTsPrInG")).isEqualTo("tEsT");
		assertThat(generator.generateContributorName("springboot")).isEqualTo("spring");
		assertThat(generator.generateContributorName("springspring")).isEqualTo("spring");
		assertThat(generator.generateContributorName("test")).isEqualTo("test");
	}

}
