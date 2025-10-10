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

package org.springframework.boot.data.autoconfigure.web;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.data.autoconfigure.web.DataWebWebMvcTestIntegrationTests.PageableController;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Data Web and {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
@Import(PageableController.class)
class DataWebWebMvcTestIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldSupportPageable() {
		assertThat(this.mvc.get().uri("/paged").param("page", "2").param("size", "42")).hasStatusOk()
			.hasBodyTextEqualTo("2:42");
	}

	@RestController
	@SpringBootConfiguration
	static class PageableController {

		@GetMapping("/paged")
		String paged(Pageable pageable) {
			return String.format("%s:%s", pageable.getPageNumber(), pageable.getPageSize());
		}

	}

}
