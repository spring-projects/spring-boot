/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.context.properties;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationProperties}
 *
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesTests {

	@Test
	void whenJsonHasAnIntegerDefaultValueThenItRemainsAnIntegerWhenRead() {
		ConfigurationProperties properties = ConfigurationProperties
			.fromFiles(Arrays.asList(new File("src/test/resources/spring-configuration-metadata.json")));
		assertThat(properties.get("example.counter").getDefaultValue()).isEqualTo(0);
	}

}
