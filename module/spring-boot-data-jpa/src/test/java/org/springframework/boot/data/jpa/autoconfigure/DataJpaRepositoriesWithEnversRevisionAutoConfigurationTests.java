/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.data.jpa.autoconfigure.domain.country.CountryRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataJpaRepositoriesAutoConfiguration} with Spring Data Envers on the
 * classpath.
 *
 * @author Stefano Cordio
 */
class DataJpaRepositoriesWithEnversRevisionAutoConfigurationTests
		extends AbstractDataJpaRepositoriesAutoConfigurationTests {

	@Test
	void autoConfigurationShouldSucceedWithRevisionRepository() {
		this.contextRunner.withUserConfiguration(RevisionRepositoryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(CountryRepository.class));
	}

}
