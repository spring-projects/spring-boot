/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} to validate converters are discovered.
 *
 * @author Stephane Nicoll
 */
@WebMvcTest(controllers = ExampleController2.class)
@WithMockUser
class WebMvcTestConverterIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldFindConverter() {
		String id = UUID.randomUUID().toString();
		assertThat(this.mvc.get().uri("/two/" + id)).hasStatusOk().hasBodyTextEqualTo(id + "two");
	}

}
