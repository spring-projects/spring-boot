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

import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JacksonTester}. Shows typical usage.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class JacksonTesterIntegrationTests {

	private JacksonTester<ExampleObject> simpleJson;

	private JacksonTester<ExampleObjectWithView> jsonWithView;

	private JacksonTester<List<ExampleObject>> listJson;

	private JacksonTester<Map<String, Integer>> mapJson;

	private ObjectMapper objectMapper;

	private static final String JSON = "{\"name\":\"Spring\",\"age\":123}";

	@Before
	public void setup() {
		this.objectMapper = new ObjectMapper();
		JacksonTester.initFields(this, this.objectMapper);
	}

	@Test
	public void typicalTest() throws Exception {
		String example = JSON;
		assertThat(this.simpleJson.parse(example).getObject().getName())
				.isEqualTo("Spring");
	}

	@Test
	public void typicalListTest() throws Exception {
		String example = "[" + JSON + "]";
		assertThat(this.listJson.parse(example)).asList().hasSize(1);
		assertThat(this.listJson.parse(example).getObject().get(0).getName())
				.isEqualTo("Spring");
	}

	@Test
	public void typicalMapTest() throws Exception {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("a", 1);
		map.put("b", 2);
		assertThat(this.mapJson.write(map)).extractingJsonPathNumberValue("@.a")
				.isEqualTo(1);
	}

	@Test
	public void writeWithView() throws Exception {
		this.objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		ExampleObjectWithView object = new ExampleObjectWithView();
		object.setName("Spring");
		object.setAge(123);
		JsonContent<ExampleObjectWithView> content = this.jsonWithView
				.forView(ExampleObjectWithView.TestView.class).write(object);
		assertThat(content).extractingJsonPathStringValue("@.name").isEqualTo("Spring");
		assertThat(content).doesNotHaveJsonPathValue("age");
	}

	@Test
	public void readWithResourceAndView() throws Exception {
		this.objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		ByteArrayResource resource = new ByteArrayResource(JSON.getBytes());
		ObjectContent<ExampleObjectWithView> content = this.jsonWithView
				.forView(ExampleObjectWithView.TestView.class).read(resource);
		assertThat(content.getObject().getName()).isEqualTo("Spring");
		assertThat(content.getObject().getAge()).isEqualTo(0);
	}

	@Test
	public void readWithReaderAndView() throws Exception {
		this.objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		Reader reader = new StringReader(JSON);
		ObjectContent<ExampleObjectWithView> content = this.jsonWithView
				.forView(ExampleObjectWithView.TestView.class).read(reader);
		assertThat(content.getObject().getName()).isEqualTo("Spring");
		assertThat(content.getObject().getAge()).isEqualTo(0);
	}

}
