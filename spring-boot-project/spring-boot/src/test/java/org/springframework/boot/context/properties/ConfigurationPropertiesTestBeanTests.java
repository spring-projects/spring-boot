/*
 * Copyright 2012-2024 the original author or authors.
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
