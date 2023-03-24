/*
 * Copyright 2012-2023 the original author or authors.
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

import com.jayway.jsonpath.Configuration;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JsonContent}.
 *
 * @author Phillip Webb
 */
class JsonContentTests {

	private static final String JSON = "{\"name\":\"spring\", \"age\":100}";

	private static final ResolvableType TYPE = ResolvableType.forClass(ExampleObject.class);

	@Test
	void createWhenResourceLoadClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JsonContent<ExampleObject>(null, TYPE, JSON, Configuration.defaultConfiguration()))
			.withMessageContaining("ResourceLoadClass must not be null");
	}

	@Test
	void createWhenJsonIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new JsonContent<ExampleObject>(getClass(), TYPE, null, Configuration.defaultConfiguration()))
			.withMessageContaining("JSON must not be null");
	}

	@Test
	void createWhenConfigurationIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JsonContent<ExampleObject>(getClass(), TYPE, JSON, null))
			.withMessageContaining("Configuration must not be null");
	}

	@Test
	void createWhenTypeIsNullShouldCreateContent() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), null, JSON,
				Configuration.defaultConfiguration());
		assertThat(content).isNotNull();
	}

	@Test
	@SuppressWarnings("deprecation")
	void assertThatShouldReturnJsonContentAssert() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
				Configuration.defaultConfiguration());
		assertThat(content.assertThat()).isInstanceOf(JsonContentAssert.class);
	}

	@Test
	void getJsonShouldReturnJson() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
				Configuration.defaultConfiguration());
		assertThat(content.getJson()).isEqualTo(JSON);

	}

	@Test
	void toStringWhenHasTypeShouldReturnString() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
				Configuration.defaultConfiguration());
		assertThat(content.toString()).isEqualTo("JsonContent " + JSON + " created from " + TYPE);
	}

	@Test
	void toStringWhenHasNoTypeShouldReturnString() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), null, JSON,
				Configuration.defaultConfiguration());
		assertThat(content.toString()).isEqualTo("JsonContent " + JSON);
	}

}
