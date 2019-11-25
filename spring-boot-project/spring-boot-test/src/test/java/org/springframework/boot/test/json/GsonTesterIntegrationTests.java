/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link GsonTester}. Shows typical usage.
 *
 * @author Andy Wilkinson
 * @author Diego Berrueta
 */
class GsonTesterIntegrationTests {

	private GsonTester<ExampleObject> simpleJson;

	private GsonTester<List<ExampleObject>> listJson;

	private GsonTester<Map<String, Integer>> mapJson;

	private GsonTester<String> stringJson;

	private Gson gson;

	private static final String JSON = "{\"name\":\"Spring\",\"age\":123}";

	@BeforeEach
	void setup() {
		this.gson = new Gson();
		GsonTester.initFields(this, this.gson);
	}

	@Test
	void typicalTest() throws Exception {
		String example = JSON;
		assertThat(this.simpleJson.parse(example).getObject().getName()).isEqualTo("Spring");
	}

	@Test
	void typicalListTest() throws Exception {
		String example = "[" + JSON + "]";
		assertThat(this.listJson.parse(example)).asList().hasSize(1);
		assertThat(this.listJson.parse(example).getObject().get(0).getName()).isEqualTo("Spring");
	}

	@Test
	void typicalMapTest() throws Exception {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("a", 1);
		map.put("b", 2);
		assertThat(this.mapJson.write(map)).extractingJsonPathNumberValue("@.a").isEqualTo(1);
	}

	@Test
	void stringLiteral() throws Exception {
		String stringWithSpecialCharacters = "myString";
		assertThat(this.stringJson.write(stringWithSpecialCharacters)).extractingJsonPathStringValue("@")
				.isEqualTo(stringWithSpecialCharacters);
	}

}
