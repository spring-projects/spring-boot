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

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Test {@link DispatcherServlet} customizations.
 *
 * @author Stephane Nicoll
 */
@WebMvcTest
@TestPropertySource(properties = { "spring.mvc.throw-exception-if-no-handler-found=true",
		"spring.mvc.static-path-pattern=/static/**" })
class WebMvcTestCustomDispatcherServletIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void dispatcherServletIsCustomized() {
		assertThat(this.mvc.get().uri("/does-not-exist")).hasStatus(HttpStatus.BAD_REQUEST)
			.hasBodyTextEqualTo("Invalid request: /does-not-exist");
	}

}
