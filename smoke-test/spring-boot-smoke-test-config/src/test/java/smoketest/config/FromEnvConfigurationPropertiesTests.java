/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FromEnvConfigurationProperties}.
 *
 * @author Moritz Halbritter
 */
@SpringBootTest
class FromEnvConfigurationPropertiesTests {

	@Autowired
	private FromEnvConfigurationProperties properties;

	@Test
	void shouldHaveImportedValues() {
		assertThat(this.properties.getKey1()).isEqualTo("value1");
	}

}
