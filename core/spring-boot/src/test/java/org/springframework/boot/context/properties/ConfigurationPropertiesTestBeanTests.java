/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for overriding {@link ConfigurationProperties @ConfigurationProperties} with
 * {@link TestBean @TestBean}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@TestPropertySource(properties = "immutable.property=test-property-source")
class ConfigurationPropertiesTestBeanTests {

	@TestBean
	@SuppressWarnings("NullAway.Init")
	private ImmutableProperties properties;

	@Autowired
	private SomeConfiguration someConfiguration;

	@Test
	void propertiesCanBeOverriddenUsingTestBean() {
		assertThat(this.properties.property).isEqualTo("test-bean");
		assertThat(this.someConfiguration.properties.property).isEqualTo("test-bean");
	}

	static ImmutableProperties properties() {
		return new ImmutableProperties("test-bean");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ImmutableProperties.class)
	static class SomeConfiguration {

		private final ImmutableProperties properties;

		SomeConfiguration(ImmutableProperties properties) {
			this.properties = properties;
		}

	}

	@ConfigurationProperties("immutable")
	static class ImmutableProperties {

		private final String property;

		ImmutableProperties(String property) {
			this.property = property;
		}

		String getProperty() {
			return this.property;
		}

	}

}
