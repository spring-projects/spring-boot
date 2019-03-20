/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.hypermedia;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.ActuatorMediaTypes;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SpringBootHypermediaApplication.class, loader = SpringBootContextLoader.class)
@WebAppConfiguration
@TestPropertySource(properties = { "spring.jackson.serialization.indent_output=true",
		"endpoints.health.sensitive=false", "management.security.enabled=false" })
@DirtiesContext
@AutoConfigureMockMvc
@AutoConfigureRestDocs("target/generated-snippets")
public class HealthEndpointDocumentation {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void health() throws Exception {
		this.mockMvc
				.perform(get("/health")
						.accept(ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON))
				.andExpect(status().isOk()).andDo(document("health/insensitive"));
	}

}
