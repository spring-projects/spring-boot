/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} (i.e. full integration test).
 *
 * @author Phillip Webb
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.SYSTEM_ERR, printOnlyOnFailure = false)
@WithMockUser(username = "user", password = "secret")
@ExtendWith(OutputCaptureExtension.class)
class MockMvcSpringBootTestIntegrationTests {

	@MockBean
	private ExampleMockableService service;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private MockMvc mvc;

	@Test
	void shouldFindController1(CapturedOutput capturedOutput) throws Exception {
		this.mvc.perform(get("/one")).andExpect(content().string("one")).andExpect(status().isOk());
		assertThat(capturedOutput).contains("Request URI = /one");
	}

	@Test
	void shouldFindController2() throws Exception {
		this.mvc.perform(get("/two")).andExpect(content().string("hellotwo")).andExpect(status().isOk());
	}

	@Test
	void shouldFindControllerAdvice() throws Exception {
		this.mvc.perform(get("/error")).andExpect(content().string("recovered")).andExpect(status().isOk());
	}

	@Test
	void shouldHaveRealService() {
		assertThat(this.applicationContext.getBean(ExampleRealService.class)).isNotNull();
	}

}
