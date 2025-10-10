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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AbstractJsonMarshalTester}.
 *
 * @author Phillip Webb
 */
abstract class AbstractJsonMarshalTesterTests {

	private static final String JSON = "{\"name\":\"Spring\",\"age\":123}";

	private static final String MAP_JSON = "{\"a\":" + JSON + "}";

	private static final String ARRAY_JSON = "[" + JSON + "]";

	private static final ExampleObject OBJECT = createExampleObject("Spring", 123);

	private static final ResolvableType TYPE = ResolvableType.forClass(ExampleObject.class);

	@Test
	void writeShouldReturnJsonContent() throws Exception {
		JsonContent<Object> content = createTester(TYPE).write(OBJECT);
		assertThat(content).isEqualToJson(JSON);
	}

	@Test
	void writeListShouldReturnJsonContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("listOfExampleObject");
		List<ExampleObject> value = Collections.singletonList(OBJECT);
		JsonContent<Object> content = createTester(type).write(value);
		assertThat(content).isEqualToJson(ARRAY_JSON);
	}

	@Test
	void writeArrayShouldReturnJsonContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("arrayOfExampleObject");
		ExampleObject[] value = new ExampleObject[] { OBJECT };
		JsonContent<Object> content = createTester(type).write(value);
		assertThat(content).isEqualToJson(ARRAY_JSON);
	}

	@Test
	void writeMapShouldReturnJsonContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("mapOfExampleObject");
		Map<String, Object> value = new LinkedHashMap<>();
		value.put("a", OBJECT);
		JsonContent<Object> content = createTester(type).write(value);
		assertThat(content).isEqualToJson(MAP_JSON);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenResourceLoadClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> createTester(null, ResolvableType.forClass(ExampleObject.class)))
			.withMessageContaining("'resourceLoadClass' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenTypeIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> createTester(getClass(), null))
			.withMessageContaining("'type' must not be null");
	}

	@Test
	void parseBytesShouldReturnObject() throws Exception {
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.parse(JSON.getBytes())).isEqualTo(OBJECT);
	}

	@Test
	void parseStringShouldReturnObject() throws Exception {
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.parse(JSON)).isEqualTo(OBJECT);
	}

	@Test
	void readResourcePathShouldReturnObject() throws Exception {
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read("example.json")).isEqualTo(OBJECT);
	}

	@Test
	void readFileShouldReturnObject(@TempDir Path temp) throws Exception {
		File file = new File(temp.toFile(), "example.json");
		FileCopyUtils.copy(JSON.getBytes(), file);
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read(file)).isEqualTo(OBJECT);
	}

	@Test
	void readInputStreamShouldReturnObject() throws Exception {
		InputStream stream = new ByteArrayInputStream(JSON.getBytes());
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read(stream)).isEqualTo(OBJECT);
	}

	@Test
	void readResourceShouldReturnObject() throws Exception {
		Resource resource = new ByteArrayResource(JSON.getBytes());
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read(resource)).isEqualTo(OBJECT);
	}

	@Test
	void readReaderShouldReturnObject() throws Exception {
		Reader reader = new StringReader(JSON);
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read(reader)).isEqualTo(OBJECT);
	}

	@Test
	void parseListShouldReturnContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("listOfExampleObject");
		AbstractJsonMarshalTester<Object> tester = createTester(type);
		assertThat(tester.parse(ARRAY_JSON)).asInstanceOf(InstanceOfAssertFactories.LIST).containsOnly(OBJECT);
	}

	@Test
	void parseArrayShouldReturnContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("arrayOfExampleObject");
		AbstractJsonMarshalTester<Object> tester = createTester(type);
		assertThat(tester.parse(ARRAY_JSON)).asArray().containsOnly(OBJECT);
	}

	@Test
	void parseMapShouldReturnContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("mapOfExampleObject");
		AbstractJsonMarshalTester<Object> tester = createTester(type);
		assertThat(tester.parse(MAP_JSON)).asMap().containsEntry("a", OBJECT);
	}

	protected static ExampleObject createExampleObject(String name, int age) {
		ExampleObject exampleObject = new ExampleObject();
		exampleObject.setName(name);
		exampleObject.setAge(age);
		return exampleObject;
	}

	protected final AbstractJsonMarshalTester<Object> createTester(ResolvableType type) {
		return createTester(AbstractJsonMarshalTesterTests.class, type);
	}

	protected abstract AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type);

	/**
	 * Access to field backed by {@link ResolvableType}.
	 */
	static class ResolvableTypes {

		public @Nullable List<ExampleObject> listOfExampleObject;

		public ExampleObject @Nullable [] arrayOfExampleObject;

		public @Nullable Map<String, ExampleObject> mapOfExampleObject;

		static ResolvableType get(String name) {
			Field field = ReflectionUtils.findField(ResolvableTypes.class, name);
			assertThat(field).isNotNull();
			return ResolvableType.forField(field);
		}

	}

}
