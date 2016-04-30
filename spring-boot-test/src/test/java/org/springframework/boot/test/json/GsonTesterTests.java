/*
 * Copyright 2012-2016 the original author or authors.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GsonTester}.
 *
 * @author Phillip Webb
 */
public class GsonTesterTests extends AbstractJsonMarshalTesterTests {

	@Test
	public void initFieldsWhenTestIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TestInstance must not be null");
		GsonTester.initFields(null, new GsonBuilder().create());
	}

	@Test
	public void initFieldsWhenMarshallerIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Marshaller must not be null");
		GsonTester.initFields(new InitFieldsTestClass(), (Gson) null);
	}

	@Test
	public void initFieldsShouldSetNullFields() {
		InitFieldsTestClass test = new InitFieldsTestClass();
		assertThat(test.test).isNull();
		assertThat(test.base).isNull();
		GsonTester.initFields(test, new GsonBuilder().create());
		assertThat(test.test).isNotNull();
		assertThat(test.base).isNotNull();
		assertThat(test.test.getType().resolve()).isEqualTo(List.class);
		assertThat(test.test.getType().resolveGeneric()).isEqualTo(ExampleObject.class);
	}

	@Override
	protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass,
			ResolvableType type) {
		return new GsonTester<Object>(resourceLoadClass, type,
				new GsonBuilder().create());
	}

	static abstract class InitFieldsBaseClass {

		public GsonTester<ExampleObject> base;

		public GsonTester<ExampleObject> baseSet = new GsonTester<ExampleObject>(
				InitFieldsBaseClass.class, ResolvableType.forClass(ExampleObject.class),
				new GsonBuilder().create());

	}

	static class InitFieldsTestClass extends InitFieldsBaseClass {

		public GsonTester<List<ExampleObject>> test;

		public GsonTester<ExampleObject> testSet = new GsonTester<ExampleObject>(
				InitFieldsBaseClass.class, ResolvableType.forClass(ExampleObject.class),
				new GsonBuilder().create());

	}

}
