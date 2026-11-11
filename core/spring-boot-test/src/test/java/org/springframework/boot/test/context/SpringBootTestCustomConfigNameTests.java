/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with a custom config name
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(properties = "spring.config.name=custom-config-name")
class SpringBootTestCustomConfigNameTests {

	@Value("${test.foo}")
	private @Nullable String foo;

	@Test
	void propertyIsLoadedFromConfigFileWithCustomName() {
		assertThat(this.foo).isEqualTo("bar");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

}
