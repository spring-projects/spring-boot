/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.json;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.app.ExampleBasicObject;
import org.springframework.boot.test.autoconfigure.json.app.ExampleCustomObject;
import org.springframework.boot.test.autoconfigure.json.app.ExampleJsonApplication;
import org.springframework.boot.test.autoconfigure.json.app.ExampleJsonObjectWithView;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JsonTest}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@RunWith(SpringRunner.class)
@JsonTest
@ContextConfiguration(classes = ExampleJsonApplication.class)
public class JsonTestIntegrationTests {

	@Autowired
	private BasicJsonTester basicJson;

	@Autowired
	private JacksonTester<ExampleBasicObject> jacksonBasicJson;

	@Autowired
	private JacksonTester<ExampleJsonObjectWithView> jacksonWithViewJson;

	@Autowired
	private JacksonTester<ExampleCustomObject> jacksonCustomJson;

	@Autowired
	private GsonTester<ExampleBasicObject> gsonJson;

	@Test
	public void basicJson() throws Exception {
		assertThat(this.basicJson.from("{\"a\":\"b\"}")).hasJsonPathStringValue("@.a");
	}

	@Test
	public void jacksonBasic() throws Exception {
		ExampleBasicObject object = new ExampleBasicObject();
		object.setValue("spring");
		assertThat(this.jacksonBasicJson.write(object)).isEqualToJson("example.json");
	}

	@Test
	public void jacksonCustom() throws Exception {
		ExampleCustomObject object = new ExampleCustomObject("spring");
		assertThat(this.jacksonCustomJson.write(object)).isEqualToJson("example.json");
	}

	@Test
	public void gson() throws Exception {
		ExampleBasicObject object = new ExampleBasicObject();
		object.setValue("spring");
		assertThat(this.gsonJson.write(object)).isEqualToJson("example.json");
	}

	@Test
	public void customView() throws Exception {
		ExampleJsonObjectWithView object = new ExampleJsonObjectWithView();
		object.setValue("spring");
		JsonContent<ExampleJsonObjectWithView> content = this.jacksonWithViewJson
				.forView(ExampleJsonObjectWithView.TestView.class).write(object);
		assertThat(content).doesNotHaveJsonPathValue("id");
		assertThat(content).isEqualToJson("example.json");
	}

}
