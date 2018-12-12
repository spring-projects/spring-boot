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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
public abstract class AbstractJsonMarshalTesterTests {

	private static final String JSON = "{\"name\":\"Spring\",\"age\":123}";

	private static final String MAP_JSON = "{\"a\":" + JSON + "}";

	private static final String ARRAY_JSON = "[" + JSON + "]";

	private static final ExampleObject OBJECT = createExampleObject("Spring", 123);

	private static final ResolvableType TYPE = ResolvableType
			.forClass(ExampleObject.class);

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void writeShouldReturnJsonContent() throws Exception {
		JsonContent<Object> content = createTester(TYPE).write(OBJECT);
		assertThat(content).isEqualToJson(JSON);
	}

	@Test
	public void writeListShouldReturnJsonContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("listOfExampleObject");
		List<ExampleObject> value = Collections.singletonList(OBJECT);
		JsonContent<Object> content = createTester(type).write(value);
		assertThat(content).isEqualToJson(ARRAY_JSON);
	}

	@Test
	public void writeArrayShouldReturnJsonContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("arrayOfExampleObject");
		ExampleObject[] value = new ExampleObject[] { OBJECT };
		JsonContent<Object> content = createTester(type).write(value);
		assertThat(content).isEqualToJson(ARRAY_JSON);
	}

	@Test
	public void writeMapShouldReturnJsonContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("mapOfExampleObject");
		Map<String, Object> value = new LinkedHashMap<>();
		value.put("a", OBJECT);
		JsonContent<Object> content = createTester(type).write(value);
		assertThat(content).isEqualToJson(MAP_JSON);
	}

	@Test
	public void createWhenResourceLoadClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> createTester(null, ResolvableType.forClass(ExampleObject.class)))
				.withMessageContaining("ResourceLoadClass must not be null");
	}

	@Test
	public void createWhenTypeIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> createTester(getClass(), null))
				.withMessageContaining("Type must not be null");
	}

	@Test
	public void parseBytesShouldReturnObject() throws Exception {
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.parse(JSON.getBytes())).isEqualTo(OBJECT);
	}

	@Test
	public void parseStringShouldReturnObject() throws Exception {
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.parse(JSON)).isEqualTo(OBJECT);
	}

	@Test
	public void readResourcePathShouldReturnObject() throws Exception {
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read("example.json")).isEqualTo(OBJECT);
	}

	@Test
	public void readFileShouldReturnObject() throws Exception {
		File file = this.temp.newFile("example.json");
		FileCopyUtils.copy(JSON.getBytes(), file);
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read(file)).isEqualTo(OBJECT);
	}

	@Test
	public void readInputStreamShouldReturnObject() throws Exception {
		InputStream stream = new ByteArrayInputStream(JSON.getBytes());
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read(stream)).isEqualTo(OBJECT);
	}

	@Test
	public void readResourceShouldReturnObject() throws Exception {
		Resource resource = new ByteArrayResource(JSON.getBytes());
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read(resource)).isEqualTo(OBJECT);
	}

	@Test
	public void readReaderShouldReturnObject() throws Exception {
		Reader reader = new StringReader(JSON);
		AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
		assertThat(tester.read(reader)).isEqualTo(OBJECT);
	}

	@Test
	public void parseListShouldReturnContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("listOfExampleObject");
		AbstractJsonMarshalTester<Object> tester = createTester(type);
		assertThat(tester.parse(ARRAY_JSON)).asList().containsOnly(OBJECT);
	}

	@Test
	public void parseArrayShouldReturnContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("arrayOfExampleObject");
		AbstractJsonMarshalTester<Object> tester = createTester(type);
		assertThat(tester.parse(ARRAY_JSON)).asArray().containsOnly(OBJECT);
	}

	@Test
	public void parseMapShouldReturnContent() throws Exception {
		ResolvableType type = ResolvableTypes.get("mapOfExampleObject");
		AbstractJsonMarshalTester<Object> tester = createTester(type);
		assertThat(tester.parse(MAP_JSON)).asMap().containsEntry("a", OBJECT);
	}

	protected static final ExampleObject createExampleObject(String name, int age) {
		ExampleObject exampleObject = new ExampleObject();
		exampleObject.setName(name);
		exampleObject.setAge(age);
		return exampleObject;
	}

	protected final AbstractJsonMarshalTester<Object> createTester(ResolvableType type) {
		return createTester(AbstractJsonMarshalTesterTests.class, type);
	}

	protected abstract AbstractJsonMarshalTester<Object> createTester(
			Class<?> resourceLoadClass, ResolvableType type);

	/**
	 * Access to field backed by {@link ResolvableType}.
	 */
	public static class ResolvableTypes {

		public List<ExampleObject> listOfExampleObject;

		public ExampleObject[] arrayOfExampleObject;

		public Map<String, ExampleObject> mapOfExampleObject;

		public static ResolvableType get(String name) {
			Field field = ReflectionUtils.findField(ResolvableTypes.class, name);
			return ResolvableType.forField(field);
		}

	}

}
