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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DynamicTestProperty}
 *
 * @author Anatoliy Korovin
 */
@SpringBootTest(classes = DynamicTestPropertyTests.class)
class DynamicTestPropertyTests {

	@DynamicTestProperty
	private static TestPropertyValues dynamicProperties() {
		return TestPropertyValues.of("a=123", String.format("b=%d", 456));
	}

	@Value("${a}")
	public String a;

	@Value("${b}")
	public String b;

	@Test
	void testPropertyValues() {
		assertThat(this.a).isEqualTo("123");
		assertThat(this.b).isEqualTo("456");
	}

}
