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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonContent}.
 *
 * @author Phillip Webb
 */
public class JsonContentTests {

	private static final String JSON = "{\"name\":\"spring\", \"age\":100}";

	private static final ResolvableType TYPE = ResolvableType
			.forClass(ExampleObject.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenResourceLoadClassIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ResourceLoadClass must not be null");
		new JsonContent<ExampleObject>(null, TYPE, JSON);
	}

	@Test
	public void createWhenJsonIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("JSON must not be null");
		new JsonContent<ExampleObject>(getClass(), TYPE, null);
	}

	@Test
	public void createWhenTypeIsNullShouldCreateContent() throws Exception {
		JsonContent<ExampleObject> content = new JsonContent<ExampleObject>(getClass(),
				null, JSON);
		assertThat(content).isNotNull();
	}

	@Test
	public void assertThatShouldReturnJsonContentAssert() throws Exception {
		JsonContent<ExampleObject> content = new JsonContent<ExampleObject>(getClass(),
				TYPE, JSON);
		assertThat(content.assertThat()).isInstanceOf(JsonContentAssert.class);
	}

	@Test
	public void getJsonShouldReturnJson() throws Exception {
		JsonContent<ExampleObject> content = new JsonContent<ExampleObject>(getClass(),
				TYPE, JSON);
		assertThat(content.getJson()).isEqualTo(JSON);

	}

	@Test
	public void toStringWhenHasTypeShouldReturnString() throws Exception {
		JsonContent<ExampleObject> content = new JsonContent<ExampleObject>(getClass(),
				TYPE, JSON);
		assertThat(content.toString())
				.isEqualTo("JsonContent " + JSON + " created from " + TYPE);
	}

	@Test
	public void toStringWhenHasNoTypeShouldReturnString() throws Exception {
		JsonContent<ExampleObject> content = new JsonContent<ExampleObject>(getClass(),
				null, JSON);
		assertThat(content.toString()).isEqualTo("JsonContent " + JSON);
	}

}
