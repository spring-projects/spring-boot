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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} (i.e. full integration test).
 * <p>
 * This uses {@link MockMvcTester} (AssertJ integration).
 *
 * @author Stephane Nicoll
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.SYSTEM_ERR, printOnlyOnFailure = false)
@WithMockUser(username = "user", password = "secret")
@ExtendWith(OutputCaptureExtension.class)
class MockMvcTesterSpringBootTestIntegrationTests {

	@MockitoBean
	private ExampleMockableService service;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldFindController1(CapturedOutput output) {
		assertThat(this.mvc.get().uri("/one")).hasStatusOk().hasBodyTextEqualTo("one");
		assertThat(output).contains("Request URI = /one");
	}

	@Test
	void shouldFindController2() {
		assertThat(this.mvc.get().uri("/two")).hasStatusOk().hasBodyTextEqualTo("hellotwo");
	}

	@Test
	void shouldFindControllerAdvice() {
		assertThat(this.mvc.get().uri("/error")).hasStatusOk().hasBodyTextEqualTo("recovered");
	}

	@Test
	void shouldHaveRealService() {
		assertThat(this.applicationContext.getBean(ExampleRealService.class)).isNotNull();
	}

	@Test
	void shouldTestWithWebTestClient(@Autowired WebTestClient webTestClient) {
		webTestClient.get().uri("/one").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("one");
	}

	@Test
	void shouldNotFailIfFormattingValueThrowsException(CapturedOutput output) {
		assertThat(this.mvc.get().uri("/formatting")).hasStatusOk().hasBodyTextEqualTo("formatting");
		assertThat(output).contains(
				"Session Attrs = << Exception 'java.lang.IllegalStateException: Formatting failed' occurred while formatting >>");
	}

}
