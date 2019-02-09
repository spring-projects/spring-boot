/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Test;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JacksonTester}.
 *
 * @author Phillip Webb
 */
public class JacksonTesterTests extends AbstractJsonMarshalTesterTests {

	@Test
	public void initFieldsWhenTestIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> JacksonTester.initFields(null, new ObjectMapper()))
				.withMessageContaining("TestInstance must not be null");
	}

	@Test
	public void initFieldsWhenMarshallerIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> JacksonTester.initFields(new InitFieldsTestClass(),
						(ObjectMapper) null))
				.withMessageContaining("Marshaller must not be null");
	}

	@Test
	public void initFieldsShouldSetNullFields() {
		InitFieldsTestClass test = new InitFieldsTestClass();
		assertThat(test.test).isNull();
		assertThat(test.base).isNull();
		JacksonTester.initFields(test, new ObjectMapper());
		assertThat(test.test).isNotNull();
		assertThat(test.base).isNotNull();
		assertThat(test.test.getType().resolve()).isEqualTo(List.class);
		assertThat(test.test.getType().resolveGeneric()).isEqualTo(ExampleObject.class);
	}

	@Override
	protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass,
			ResolvableType type) {
		return new JacksonTester<>(resourceLoadClass, type, new ObjectMapper());
	}

	abstract static class InitFieldsBaseClass {

		public JacksonTester<ExampleObject> base;

		public JacksonTester<ExampleObject> baseSet = new JacksonTester<>(
				InitFieldsBaseClass.class, ResolvableType.forClass(ExampleObject.class),
				new ObjectMapper());

	}

	static class InitFieldsTestClass extends InitFieldsBaseClass {

		public JacksonTester<List<ExampleObject>> test;

		public JacksonTester<ExampleObject> testSet = new JacksonTester<>(
				InitFieldsBaseClass.class, ResolvableType.forClass(ExampleObject.class),
				new ObjectMapper());

	}

}
