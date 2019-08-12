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

package org.springframework.boot.test.context;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WebAppConfiguration @WebAppConfiguration} integration.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext
@ContextConfiguration(loader = SpringBootContextLoader.class)
@WebAppConfiguration
class SpringBootContextLoaderMockMvcTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ServletContext servletContext;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	void testMockHttpEndpoint() throws Exception {
		this.mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string("Hello World"));
	}

	@Test
	void validateWebApplicationContextIsSet() {
		assertThat(this.context).isSameAs(WebApplicationContextUtils.getWebApplicationContext(this.servletContext));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	@RestController
	static class Config {

		@RequestMapping("/")
		String home() {
			return "Hello World";
		}

	}

}
