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

package smoketest.jackson2.only;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.ObjectContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleJackson2OnlyApplication} using {@link JsonTest @JsonTest}.
 *
 * @author Andy Wilkinson
 */
@JsonTest
@SuppressWarnings({ "deprecation", "removal" })
class SampleJackson2OnlyApplicationJsonTests {

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	org.springframework.boot.test.json.Jackson2Tester<JsonPojo> tester;

	@Test
	void objectMapperIsInjected() {
		assertThat(this.objectMapper).isNotNull();
	}

	@Test
	void jacksonTesterWorks() throws IOException {
		ObjectContent<JsonPojo> pojo = this.tester.parse("{\"alpha\":\"a\",\"bravo\":\"b\"}");
		assertThat(pojo).extracting(JsonPojo::getAlpha).isEqualTo("a");
		assertThat(pojo).extracting(JsonPojo::getBravo).isEqualTo("b");
	}

}
