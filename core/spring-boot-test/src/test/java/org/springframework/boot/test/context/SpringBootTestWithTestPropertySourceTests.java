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

package org.springframework.boot.test.context;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for using {@link SpringBootTest @SpringBootTest} with
 * {@link TestPropertySource @TestPropertySource}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.NONE,
		properties = { "boot-test-inlined=foo", "b=boot-test-inlined", "c=boot-test-inlined" })
@TestPropertySource(
		properties = { "property-source-inlined=bar", "a=property-source-inlined", "c=property-source-inlined" },
		locations = "classpath:/test-property-source-annotation.properties")
class SpringBootTestWithTestPropertySourceTests {

	@Autowired
	private Config config;

	@Test
	void propertyFromSpringBootTestProperties() {
		assertThat(this.config.bootTestInlined).isEqualTo("foo");
	}

	@Test
	void propertyFromTestPropertySourceProperties() {
		assertThat(this.config.propertySourceInlined).isEqualTo("bar");
	}

	@Test
	void propertyFromTestPropertySourceLocations() {
		assertThat(this.config.propertySourceLocation).isEqualTo("baz");
	}

	@Test
	void propertyFromPropertySourcePropertiesOverridesPropertyFromPropertySourceLocations() {
		assertThat(this.config.propertySourceInlinedOverridesPropertySourceLocation)
			.isEqualTo("property-source-inlined");
	}

	@Test
	void propertyFromBootTestPropertiesOverridesPropertyFromPropertySourceLocations() {
		assertThat(this.config.bootTestInlinedOverridesPropertySourceLocation).isEqualTo("boot-test-inlined");
	}

	@Test
	void propertyFromPropertySourcePropertiesOverridesPropertyFromBootTestProperties() {
		assertThat(this.config.propertySourceInlinedOverridesBootTestInlined).isEqualTo("property-source-inlined");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Value("${boot-test-inlined}")
		private @Nullable String bootTestInlined;

		@Value("${property-source-inlined}")
		private @Nullable String propertySourceInlined;

		@Value("${property-source-location}")
		private @Nullable String propertySourceLocation;

		@Value("${a}")
		private @Nullable String propertySourceInlinedOverridesPropertySourceLocation;

		@Value("${b}")
		private @Nullable String bootTestInlinedOverridesPropertySourceLocation;

		@Value("${c}")
		private @Nullable String propertySourceInlinedOverridesBootTestInlined;

		@Bean
		static PropertySourcesPlaceholderConfigurer propertyPlaceholder() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

}
