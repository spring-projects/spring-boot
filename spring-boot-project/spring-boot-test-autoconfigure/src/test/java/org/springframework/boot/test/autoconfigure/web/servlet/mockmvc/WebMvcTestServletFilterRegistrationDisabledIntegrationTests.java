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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Tests for {@link WebMvcTest} with a disabled filter registration.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@WebMvcTest
public class WebMvcTestServletFilterRegistrationDisabledIntegrationTests {

	@Autowired
	private MockMvc mvc;

	@Test
	public void shouldNotApplyFilter() throws Exception {
		this.mvc.perform(get("/one")).andExpect(header().string("x-test", (String) null));
	}

	@TestConfiguration
	static class DisabledRegistrationConfiguration {

		@Bean
		public FilterRegistrationBean<ExampleFilter> exampleFilterRegistration(ExampleFilter filter) {
			FilterRegistrationBean<ExampleFilter> registration = new FilterRegistrationBean<>(filter);
			registration.setEnabled(false);
			return registration;
		}

	}

}
