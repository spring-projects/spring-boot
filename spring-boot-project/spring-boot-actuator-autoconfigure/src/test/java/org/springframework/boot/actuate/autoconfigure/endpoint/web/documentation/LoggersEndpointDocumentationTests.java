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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

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
class LoggersEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final List<FieldDescriptor> levelFields = Arrays.asList(
			fieldWithPath("configuredLevel").description("Configured level of the logger, if any.").optional(),
			fieldWithPath("effectiveLevel").description("Effective level of the logger."));

	private static final List<FieldDescriptor> groupLevelFields;

	static {
		groupLevelFields = Arrays.asList(
				fieldWithPath("configuredLevel").description("Configured level of the logger group, if any.")
						.type(JsonFieldType.STRING).optional(),
				fieldWithPath("members").description("Loggers that are part of this group"));
	}

	@MockBean
	private LoggingSystem loggingSystem;

	@Autowired
	private LoggerGroups loggerGroups;

	@Test
	void allLoggers() throws Exception {
		given(this.loggingSystem.getSupportedLogLevels()).willReturn(EnumSet.allOf(LogLevel.class));
		given(this.loggingSystem.getLoggerConfigurations())
				.willReturn(Arrays.asList(new LoggerConfiguration("ROOT", LogLevel.INFO, LogLevel.INFO),
						new LoggerConfiguration("com.example", LogLevel.DEBUG, LogLevel.DEBUG)));
		this.mockMvc.perform(get("/actuator/loggers")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("loggers/all",
						responseFields(fieldWithPath("levels").description("Levels support by the logging system."),
								fieldWithPath("loggers").description("Loggers keyed by name."),
								fieldWithPath("groups").description("Logger groups keyed by name"))
										.andWithPrefix("loggers.*.", levelFields)
										.andWithPrefix("groups.*.", groupLevelFields)));
	}

	@Test
	void logger() throws Exception {
		given(this.loggingSystem.getLoggerConfiguration("com.example"))
				.willReturn(new LoggerConfiguration("com.example", LogLevel.INFO, LogLevel.INFO));
		this.mockMvc.perform(get("/actuator/loggers/com.example")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("loggers/single", responseFields(levelFields)));
	}

	@Test
	void loggerGroups() throws Exception {
		this.loggerGroups.get("test").configureLogLevel(LogLevel.INFO, (member, level) -> {
		});
		this.mockMvc.perform(get("/actuator/loggers/test")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("loggers/group", responseFields(groupLevelFields)));
		resetLogger();
	}

	@Test
	void setLogLevel() throws Exception {
		this.mockMvc
				.perform(post("/actuator/loggers/com.example").content("{\"configuredLevel\":\"debug\"}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent())
				.andDo(MockMvcRestDocumentation.document("loggers/set", requestFields(fieldWithPath("configuredLevel")
						.description("Level for the logger. May be omitted to clear the level.").optional())));
		verify(this.loggingSystem).setLogLevel("com.example", LogLevel.DEBUG);
	}

	@Test
	void setLogLevelOfLoggerGroup() throws Exception {
		this.mockMvc
				.perform(post("/actuator/loggers/test")
						.content("{\"configuredLevel\":\"debug\"}").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent()).andDo(
						MockMvcRestDocumentation.document("loggers/setGroup",
								requestFields(fieldWithPath("configuredLevel").description(
										"Level for the logger group. May be omitted to clear the level of the loggers.")
										.optional())));
		verify(this.loggingSystem).setLogLevel("test.member1", LogLevel.DEBUG);
		verify(this.loggingSystem).setLogLevel("test.member2", LogLevel.DEBUG);
		resetLogger();
	}

	private void resetLogger() {
		this.loggerGroups.get("test").configureLogLevel(LogLevel.INFO, (a, b) -> {
		});
	}

	@Test
	void clearLogLevel() throws Exception {
		this.mockMvc
				.perform(post("/actuator/loggers/com.example").content("{}").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent()).andDo(MockMvcRestDocumentation.document("loggers/clear"));
		verify(this.loggingSystem).setLogLevel("com.example", null);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		LoggersEndpoint endpoint(LoggingSystem loggingSystem, LoggerGroups groups) {
			groups.putAll(getLoggerGroups());
			groups.get("test").configureLogLevel(LogLevel.INFO, (member, level) -> {
			});
			return new LoggersEndpoint(loggingSystem, groups);
		}

		private Map<String, List<String>> getLoggerGroups() {
			return Collections.singletonMap("test", Arrays.asList("test.member1", "test.member2"));
		}

	}

}
