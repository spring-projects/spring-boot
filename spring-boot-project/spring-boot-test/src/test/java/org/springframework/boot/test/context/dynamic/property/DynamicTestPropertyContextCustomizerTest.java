package org.springframework.boot.test.context.dynamic.property;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link DynamicTestPropertyContextCustomizer}
 *
 * @author Anatoliy Korovin
 */
class DynamicTestPropertyContextCustomizerTest {

	@Test
	void equalsOfContextCustomizers() {

		DynamicTestPropertyContextCustomizer firstContextCustomizer =
				new DynamicTestPropertyContextCustomizer(
						Arrays.asList(TestPropertyValues.of("a=123"),
									  TestPropertyValues.of("b=456")));

		DynamicTestPropertyContextCustomizer secondContextCustomizer =
				new DynamicTestPropertyContextCustomizer(
						Arrays.asList(TestPropertyValues.of("b=456"),
									  TestPropertyValues.of("a=123")));

		assertThat(firstContextCustomizer).isEqualTo(secondContextCustomizer);
	}

	@Test
	void notEqualsContextCustomizers() {

		DynamicTestPropertyContextCustomizer firstContextCustomizer =
				new DynamicTestPropertyContextCustomizer(
						Arrays.asList(TestPropertyValues.of("a=123"),
									  TestPropertyValues.of("b=456")));

		DynamicTestPropertyContextCustomizer secondContextCustomizer =
				new DynamicTestPropertyContextCustomizer(
						Arrays.asList(TestPropertyValues.of("b=456"),
									  TestPropertyValues.of("a=0")));

		assertThat(firstContextCustomizer).isNotEqualTo(secondContextCustomizer);
	}
}