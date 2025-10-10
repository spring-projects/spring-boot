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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JsonbTester}.
 *
 * @author Eddú Meléndez
 */
class JsonbTesterTests extends AbstractJsonMarshalTesterTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void initFieldsWhenTestIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> JsonbTester.initFields(null, JsonbBuilder.create()))
			.withMessageContaining("'testInstance' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void initFieldsWhenMarshallerIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> JsonbTester.initFields(new InitFieldsTestClass(), (Jsonb) null))
			.withMessageContaining("'marshaller' must not be null");
	}

	@Test
	void initFieldsShouldSetNullFields() {
		InitFieldsTestClass test = new InitFieldsTestClass();
		assertThat(test.test).isNull();
		assertThat(test.base).isNull();
		JsonbTester.initFields(test, JsonbBuilder.create());
		assertThat(test.test).isNotNull();
		assertThat(test.base).isNotNull();
		ResolvableType type = test.test.getType();
		assertThat(type).isNotNull();
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.resolveGeneric()).isEqualTo(ExampleObject.class);
	}

	@Override
	protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type) {
		return new JsonbTester<>(resourceLoadClass, type, JsonbBuilder.create());
	}

	abstract static class InitFieldsBaseClass {

		public @Nullable JsonbTester<ExampleObject> base;

		public JsonbTester<ExampleObject> baseSet = new JsonbTester<>(InitFieldsBaseClass.class,
				ResolvableType.forClass(ExampleObject.class), JsonbBuilder.create());

	}

	static class InitFieldsTestClass extends InitFieldsBaseClass {

		public @Nullable JsonbTester<List<ExampleObject>> test;

		public JsonbTester<ExampleObject> testSet = new JsonbTester<>(InitFieldsBaseClass.class,
				ResolvableType.forClass(ExampleObject.class), JsonbBuilder.create());

	}

}
