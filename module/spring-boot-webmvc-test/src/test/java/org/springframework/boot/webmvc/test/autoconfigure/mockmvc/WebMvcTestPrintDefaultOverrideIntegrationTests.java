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
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} when a specific controller is defined.
 *
 * @author Phillip Webb
 */
@WebMvcTest
@TestPropertySource(properties = "spring.test.mockmvc.print=NONE")
@ExtendWith(OutputCaptureExtension.class)
class WebMvcTestPrintDefaultOverrideIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldFindController1(CapturedOutput output) {
		assertThat(this.mvc.get().uri("/one")).hasStatusOk().hasBodyTextEqualTo("one");
		assertThat(output).doesNotContain("Request URI = /one");
	}

}
