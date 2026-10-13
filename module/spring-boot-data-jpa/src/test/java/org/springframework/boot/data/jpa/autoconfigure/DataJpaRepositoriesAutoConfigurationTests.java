/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataJpaRepositoriesAutoConfiguration} without Spring Data Envers on
 * the classpath.
 *
 * @author Stefano Cordio
 */
@ClassPathExclusions("spring-data-envers-*.jar")
class DataJpaRepositoriesAutoConfigurationTests extends AbstractDataJpaRepositoriesAutoConfigurationTests {

	@Test
	void autoConfigurationShouldFailWithRevisionRepository() {
		this.contextRunner.withUserConfiguration(RevisionRepositoryConfiguration.class)
			.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class));
	}

}
