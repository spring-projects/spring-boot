/*
 * Copyright 2012-present the original author or authors.
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
