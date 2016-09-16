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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JacksonTester}. Shows typical usage.
 *
 * @author Phillip Webb
 */
public class JacksonTesterIntegrationTests {

	private JacksonTester<List<ExampleObject>> listJson;

	private JacksonTester<Map<String, Integer>> mapJson;

	@Before
	public void setup() {
		JacksonTester.initFields(this, new ObjectMapper());
	}

	@Test
	public void typicalListTest() throws Exception {
		String example = "[{\"name\":\"Spring\",\"age\":123}]";
		assertThat(this.listJson.parse(example)).asList().hasSize(1);
		assertThat(this.listJson.parse(example).getObject().get(0).getName())
				.isEqualTo("Spring");
	}

	@Test
	public void typicalMapTest() throws Exception {
		Map<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("a", 1);
		map.put("b", 2);
		assertThat(this.mapJson.write(map)).extractingJsonPathNumberValue("@.a")
				.isEqualTo(1);
	}

}
