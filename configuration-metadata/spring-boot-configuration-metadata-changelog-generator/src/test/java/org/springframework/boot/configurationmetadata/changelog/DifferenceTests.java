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

package org.springframework.boot.configurationmetadata.changelog;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Difference}.
 *
 * @author Stephane Nicoll
 */
class DifferenceTests {

	private static ConfigurationMetadataProperty createProperty(String id, Object defaultValue) {
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId(id);
		property.setDefaultValue(defaultValue);
		return property;
	}

	@Nested
	class DefaultChangedTests {

		@Test
		void sameValueComputesNoDifference() {
			Difference difference = Difference.compute(createProperty("test.id", "test"),
					createProperty("test.id", "test"));
			assertThat(difference).isNull();
		}

		@Test
		void bothNullComputesNoDifference() {
			Difference difference = Difference.compute(createProperty("test.id", null),
					createProperty("test.id", null));
			assertThat(difference).isNull();
		}

		@Test
		void nullThenNotNullDefaultChanged() {
			Difference difference = Difference.compute(createProperty("test.id", null),
					createProperty("test.id", "test"));
			assertThat(difference).isNotNull();
			assertThat(difference.type()).isEqualTo(DifferenceType.DEFAULT_CHANGED);
		}

		@Test
		void notNullThenNullDefaultChanged() {
			Difference difference = Difference.compute(createProperty("test.id", "test"),
					createProperty("test.id", null));
			assertThat(difference).isNotNull();
			assertThat(difference.type()).isEqualTo(DifferenceType.DEFAULT_CHANGED);
		}

		@Test
		void arrayEqualsComputesNoDifference() {
			Difference difference = Difference.compute(createProperty("test.id", new Object[] { "one", "two" }),
					createProperty("test.id", new Object[] { "one", "two" }));
			assertThat(difference).isNull();
		}

		@Test
		void arrayOrderChangedComputesDefaultChanged() {
			Difference difference = Difference.compute(createProperty("test.id", new Object[] { "one", "two" }),
					createProperty("test.id", new Object[] { "two", "one" }));
			assertThat(difference).isNotNull();
			assertThat(difference.type()).isEqualTo(DifferenceType.DEFAULT_CHANGED);
		}

		@Test
		void arrayAdditionalValueComputesDefaultChanged() {
			Difference difference = Difference.compute(createProperty("test.id", new Object[] { "one", "two" }),
					createProperty("test.id", new Object[] { "one", "two", "three" }));
			assertThat(difference).isNotNull();
			assertThat(difference.type()).isEqualTo(DifferenceType.DEFAULT_CHANGED);
		}

	}

}
