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

package org.springframework.boot.test.autoconfigure.xml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link XmlTest#properties properties} attribute of
 * {@link XmlTest @XmlTest}.
 *
 * @author Tiziano Basile
 */
@XmlTest(properties = { "spring.profiles.active=test", "example.property=example-value" })
class XmlTestPropertiesIntegrationTests {

	@Autowired
	private Environment environment;

	@Test
	void environmentWhenPropertiesAreDeclaredThenContainsThem() {
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
		assertThat(this.environment.getProperty("example.property")).isEqualTo("example-value");
	}

	@Nested
	class NestedTests {

		@Autowired
		private Environment innerEnvironment;

		@Test
		void propertiesFromEnclosingClassAffectNestedTests() {
			assertThat(XmlTestPropertiesIntegrationTests.this.environment.getActiveProfiles()).containsExactly("test");
			assertThat(this.innerEnvironment.getActiveProfiles()).containsExactly("test");
			assertThat(this.innerEnvironment.getProperty("example.property")).isEqualTo("example-value");
		}

	}

}
