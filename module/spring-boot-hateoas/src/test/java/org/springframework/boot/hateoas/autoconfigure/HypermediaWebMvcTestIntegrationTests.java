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

package org.springframework.boot.hateoas.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebMvcTest @WebMvcTest} and Spring HATEOAS.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
class HypermediaWebMvcTestIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void plainResponse() {
		assertThat(this.mvc.get().uri("/hateoas/plain")).hasContentType("application/json");
	}

	@Test
	void hateoasResponse() {
		assertThat(this.mvc.get().uri("/hateoas/resource")).hasContentType("application/hal+json");
	}

	@SpringBootConfiguration
	@Import(HateoasController.class)
	static class TestConfiguration {

	}

	@RestController
	@RequestMapping("/hateoas")
	static class HateoasController {

		@RequestMapping("/resource")
		EntityModel<Map<String, String>> resource() {
			return EntityModel.of(new HashMap<>(), Link.of("self", LinkRelation.of("https://api.example.com")));
		}

		@RequestMapping("/plain")
		Map<String, String> plain() {
			return new HashMap<>();
		}

	}

}
