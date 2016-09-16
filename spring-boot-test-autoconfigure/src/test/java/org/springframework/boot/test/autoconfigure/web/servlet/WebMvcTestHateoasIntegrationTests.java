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

package org.springframework.boot.test.autoconfigure.web.servlet;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Integration tests for {@link WebMvcTest} and Spring HATEOAS.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@WebMvcTest(secure = false)
public class WebMvcTestHateoasIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void plainResponse() throws Exception {
		this.mockMvc.perform(get("/hateoas/plain")).andExpect(header()
				.string(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8"));
	}

	@Test
	public void hateoasResponse() throws Exception {
		this.mockMvc.perform(get("/hateoas/resource")).andExpect(header()
				.string(HttpHeaders.CONTENT_TYPE, "application/hal+json;charset=UTF-8"));
	}

}
