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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import org.junit.Test;

import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link LogFileWebEndpoint}.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = "logging.file.name=src/test/resources/org/springframework/boot/actuate/autoconfigure/endpoint/web/documentation/sample.log")
public class LogFileWebEndpointDocumentationTests
		extends MockMvcEndpointDocumentationTests {

	@Test
	public void logFile() throws Exception {
		this.mockMvc.perform(get("/actuator/logfile")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("logfile/entire"));
	}

	@Test
	public void logFileRange() throws Exception {
		this.mockMvc.perform(get("/actuator/logfile").header("Range", "bytes=0-1023"))
				.andExpect(status().isPartialContent())
				.andDo(MockMvcRestDocumentation.document("logfile/range"));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public LogFileWebEndpoint endpoint(Environment environment) {
			return new LogFileWebEndpoint(environment);
		}

	}

}
