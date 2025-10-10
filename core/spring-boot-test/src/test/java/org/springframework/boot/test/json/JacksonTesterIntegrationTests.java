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

import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JacksonTester}. Shows typical usage.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Diego Berrueta
 */
class JacksonTesterIntegrationTests {

	@SuppressWarnings("NullAway.Init")
	private JacksonTester<ExampleObject> simpleJson;

	@SuppressWarnings("NullAway.Init")
	private JacksonTester<ExampleObjectWithView> jsonWithView;

	@SuppressWarnings("NullAway.Init")
	private JacksonTester<List<ExampleObject>> listJson;

	@SuppressWarnings("NullAway.Init")
	private JacksonTester<Map<String, Integer>> mapJson;

	@SuppressWarnings("NullAway.Init")
	private JacksonTester<String> stringJson;

	private static final String JSON = "{\"name\":\"Spring\",\"age\":123}";

	@Test
	void typicalTest() throws Exception {
		JacksonTester.initFields(this, new JsonMapper());
		assertThat(this.simpleJson.parse(JSON).getObject().getName()).isEqualTo("Spring");
	}

	@Test
	void typicalListTest() throws Exception {
		JacksonTester.initFields(this, new JsonMapper());
		String example = "[" + JSON + "]";
		assertThat(this.listJson.parse(example)).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1);
		assertThat(this.listJson.parse(example).getObject().get(0).getName()).isEqualTo("Spring");
	}

	@Test
	void typicalMapTest() throws Exception {
		JacksonTester.initFields(this, new JsonMapper());
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("a", 1);
		map.put("b", 2);
		assertThat(this.mapJson.write(map)).extractingJsonPathNumberValue("@.a").isEqualTo(1);
	}

	@Test
	void stringLiteral() throws Exception {
		JacksonTester.initFields(this, new JsonMapper());
		String stringWithSpecialCharacters = "myString";
		assertThat(this.stringJson.write(stringWithSpecialCharacters)).extractingJsonPathStringValue("@")
			.isEqualTo(stringWithSpecialCharacters);
	}

	@Test
	void parseSpecialCharactersTest() throws Exception {
		JacksonTester.initFields(this, new JsonMapper());
		// Confirms that the handling of special characters is symmetrical between
		// the serialization (through the JacksonTester) and the parsing (through
		// json-path). By default json-path uses SimpleJson as its parser, which has a
		// slightly different behavior to Jackson and breaks the symmetry. JacksonTester
		// configures json-path to use Jackson for evaluating the path expressions and
		// restores the symmetry. See gh-15727
		String stringWithSpecialCharacters = "\u0006\u007F";
		assertThat(this.stringJson.write(stringWithSpecialCharacters)).extractingJsonPathStringValue("@")
			.isEqualTo(stringWithSpecialCharacters);
	}

	@Test
	void writeWithView() throws Exception {
		JacksonTester.initFields(this, JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build());
		ExampleObjectWithView object = new ExampleObjectWithView();
		object.setName("Spring");
		object.setAge(123);
		JsonContent<ExampleObjectWithView> content = this.jsonWithView.forView(ExampleObjectWithView.TestView.class)
			.write(object);
		assertThat(content).extractingJsonPathStringValue("@.name").isEqualTo("Spring");
		assertThat(content).doesNotHaveJsonPathValue("age");
	}

	@Test
	void readWithResourceAndView() throws Exception {
		JacksonTester.initFields(this, JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build());
		ByteArrayResource resource = new ByteArrayResource(JSON.getBytes());
		ObjectContent<ExampleObjectWithView> content = this.jsonWithView.forView(ExampleObjectWithView.TestView.class)
			.read(resource);
		assertThat(content.getObject().getName()).isEqualTo("Spring");
		assertThat(content.getObject().getAge()).isZero();
	}

	@Test
	void readWithReaderAndView() throws Exception {
		JacksonTester.initFields(this, JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build());
		Reader reader = new StringReader(JSON);
		ObjectContent<ExampleObjectWithView> content = this.jsonWithView.forView(ExampleObjectWithView.TestView.class)
			.read(reader);
		assertThat(content.getObject().getName()).isEqualTo("Spring");
		assertThat(content.getObject().getAge()).isZero();
	}

}
