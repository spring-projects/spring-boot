/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link LoggersEndpoint}.
 *
 * @author Andy Wilkinson
 */
public class LoggersEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final List<FieldDescriptor> levelFields = Arrays.asList(
			fieldWithPath("configuredLevel")
					.description("Configured level of the logger, if any.").optional(),
			fieldWithPath("effectiveLevel")
					.description("Effective level of the logger."));

	@MockBean
	private LoggingSystem loggingSystem;

	@Test
	public void allLoggers() throws Exception {
		given(this.loggingSystem.getSupportedLogLevels())
				.willReturn(EnumSet.allOf(LogLevel.class));
		given(this.loggingSystem.getLoggerConfigurations()).willReturn(Arrays.asList(
				new LoggerConfiguration("ROOT", LogLevel.INFO, LogLevel.INFO),
				new LoggerConfiguration("com.example", LogLevel.DEBUG, LogLevel.DEBUG)));
		this.mockMvc.perform(get("/actuator/loggers")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("loggers/all", responseFields(
						fieldWithPath("levels")
								.description("Levels support by the logging system."),
						fieldWithPath("loggers").description("Loggers keyed by name."))
								.andWithPrefix("loggers.*.", levelFields)));
	}

	@Test
	public void logger() throws Exception {
		given(this.loggingSystem.getLoggerConfiguration("com.example")).willReturn(
				new LoggerConfiguration("com.example", LogLevel.INFO, LogLevel.INFO));
		this.mockMvc.perform(get("/actuator/loggers/com.example"))
				.andExpect(status().isOk()).andDo(MockMvcRestDocumentation
						.document("loggers/single", responseFields(levelFields)));
	}

	@Test
	public void setLogLevel() throws Exception {
		this.mockMvc
				.perform(post("/actuator/loggers/com.example")
						.content("{\"configuredLevel\":\"debug\"}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent()).andDo(
						MockMvcRestDocumentation.document("loggers/set",
								requestFields(fieldWithPath("configuredLevel")
										.description("Level for the logger. May be"
												+ " omitted to clear the level.")
										.optional())));
		verify(this.loggingSystem).setLogLevel("com.example", LogLevel.DEBUG);
	}

	@Test
	public void clearLogLevel() throws Exception {
		this.mockMvc
				.perform(post("/actuator/loggers/com.example").content("{}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent())
				.andDo(MockMvcRestDocumentation.document("loggers/clear"));
		verify(this.loggingSystem).setLogLevel("com.example", null);
	}

	@Configuration
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public LoggersEndpoint endpoint(LoggingSystem loggingSystem) {
			return new LoggersEndpoint(loggingSystem);
		}

	}

}
