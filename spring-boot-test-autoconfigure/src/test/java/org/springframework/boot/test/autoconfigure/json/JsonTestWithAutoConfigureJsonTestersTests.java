/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.json;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.app.ExampleBasicObject;
import org.springframework.boot.test.autoconfigure.json.app.ExampleJsonApplication;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JsonTest} with {@link AutoConfigureJsonTesters}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@JsonTest
@AutoConfigureJsonTesters(enabled = false)
@ContextConfiguration(classes = ExampleJsonApplication.class)
public class JsonTestWithAutoConfigureJsonTestersTests {

	@Autowired(required = false)
	private BasicJsonTester basicJson;

	@Autowired(required = false)
	private JacksonTester<ExampleBasicObject> jacksonTester;

	@Autowired(required = false)
	private GsonTester<ExampleBasicObject> gsonTester;

	@Test
	public void basicJson() throws Exception {
		assertThat(this.basicJson).isNull();
	}

	@Test
	public void jackson() throws Exception {
		assertThat(this.jacksonTester).isNull();
	}

	@Test
	public void gson() throws Exception {
		assertThat(this.gsonTester).isNull();
	}

}
