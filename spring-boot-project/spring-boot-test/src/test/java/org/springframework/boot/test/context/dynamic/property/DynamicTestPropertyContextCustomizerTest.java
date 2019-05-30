/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context.dynamic.property;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicTestPropertyContextCustomizer}
 *
 * @author Anatoliy Korovin
 */
class DynamicTestPropertyContextCustomizerTest {

	@Test
	void equalsOfContextCustomizers() {

		DynamicTestPropertyContextCustomizer firstContextCustomizer = new DynamicTestPropertyContextCustomizer(
				Arrays.asList(TestPropertyValues.of("a=123"),
						TestPropertyValues.of("b=456")));

		DynamicTestPropertyContextCustomizer secondContextCustomizer = new DynamicTestPropertyContextCustomizer(
				Arrays.asList(TestPropertyValues.of("b=456"),
						TestPropertyValues.of("a=123")));

		assertThat(firstContextCustomizer).isEqualTo(secondContextCustomizer);
	}

	@Test
	void notEqualsContextCustomizers() {

		DynamicTestPropertyContextCustomizer firstContextCustomizer = new DynamicTestPropertyContextCustomizer(
				Arrays.asList(TestPropertyValues.of("a=123"),
						TestPropertyValues.of("b=456")));

		DynamicTestPropertyContextCustomizer secondContextCustomizer = new DynamicTestPropertyContextCustomizer(
				Arrays.asList(TestPropertyValues.of("b=456"),
						TestPropertyValues.of("a=0")));

		assertThat(firstContextCustomizer).isNotEqualTo(secondContextCustomizer);
	}

}
