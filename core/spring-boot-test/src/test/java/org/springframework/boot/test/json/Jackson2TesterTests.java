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

package org.springframework.boot.test.json;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Jackson2Tester}.
 *
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of JacksonTesterTests
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
class Jackson2TesterTests extends AbstractJsonMarshalTesterTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void initFieldsWhenTestIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Jackson2Tester.initFields(null, new ObjectMapper()))
			.withMessageContaining("'testInstance' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void initFieldsWhenMarshallerIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Jackson2Tester.initFields(new InitFieldsTestClass(), (ObjectMapper) null))
			.withMessageContaining("'marshaller' must not be null");
	}

	@Test
	void initFieldsShouldSetNullFields() {
		InitFieldsTestClass test = new InitFieldsTestClass();
		assertThat(test.test).isNull();
		assertThat(test.base).isNull();
		Jackson2Tester.initFields(test, new ObjectMapper());
		assertThat(test.test).isNotNull();
		assertThat(test.base).isNotNull();
		ResolvableType type = test.test.getType();
		assertThat(type).isNotNull();
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.resolveGeneric()).isEqualTo(ExampleObject.class);
	}

	@Override
	protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type) {
		return new org.springframework.boot.test.json.Jackson2Tester<>(resourceLoadClass, type, new ObjectMapper());
	}

	abstract static class InitFieldsBaseClass {

		public org.springframework.boot.test.json.@Nullable Jackson2Tester<ExampleObject> base;

		public org.springframework.boot.test.json.Jackson2Tester<ExampleObject> baseSet = new org.springframework.boot.test.json.Jackson2Tester<>(
				InitFieldsBaseClass.class, ResolvableType.forClass(ExampleObject.class), new ObjectMapper());

	}

	static class InitFieldsTestClass extends InitFieldsBaseClass {

		public org.springframework.boot.test.json.@Nullable Jackson2Tester<List<ExampleObject>> test;

		public org.springframework.boot.test.json.Jackson2Tester<ExampleObject> testSet = new org.springframework.boot.test.json.Jackson2Tester<>(
				InitFieldsBaseClass.class, ResolvableType.forClass(ExampleObject.class), new ObjectMapper());

	}

}
