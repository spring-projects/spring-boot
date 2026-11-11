/*
 * Copyright 2012-present the original author or authors.
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
