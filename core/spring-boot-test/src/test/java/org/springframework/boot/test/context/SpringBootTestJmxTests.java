/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for disabling JMX by default
 *
 * @author Dave Syer
 */
@DirtiesContext
@SpringBootTest
class SpringBootTestJmxTests {

	@Value("${spring.jmx.enabled}")
	private boolean jmx;

	@Test
	void disabledByDefault() {
		assertThat(this.jmx).isFalse();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

}
