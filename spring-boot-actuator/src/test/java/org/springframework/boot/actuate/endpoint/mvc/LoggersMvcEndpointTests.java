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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Collections;
import java.util.EnumSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.LoggersEndpoint;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link LoggersMvcEndpoint}.
 *
 * @author Ben Hale
 * @author Phillip Webb
 * @author Eddú Meléndez
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = "management.security.enabled=false")
public class LoggersMvcEndpointTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private LoggingSystem loggingSystem;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.context.getBean(LoggersEndpoint.class).setEnabled(true);
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context)
				.alwaysDo(MockMvcResultHandlers.print()).build();
	}

	@Before
	@After
	public void resetMocks() {
		Mockito.reset(this.loggingSystem);
		given(this.loggingSystem.getSupportedLogLevels())
				.willReturn(EnumSet.allOf(LogLevel.class));
	}

	@Test
	public void getLoggerShouldReturnAllLoggerConfigurations() throws Exception {
		given(this.loggingSystem.getLoggerConfigurations()).willReturn(Collections
				.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		String expected = "{\"levels\":[\"OFF\",\"FATAL\",\"ERROR\",\"WARN\",\"INFO\",\"DEBUG\",\"TRACE\"],"
				+ "\"loggers\":{\"ROOT\":{\"configuredLevel\":null,\"effectiveLevel\":\"DEBUG\"}}}";
		this.mvc.perform(get("/loggers")).andExpect(status().isOk())
				.andExpect(content().json(expected));
	}

	@Test
	public void getLoggersWhenDisabledShouldReturnNotFound() throws Exception {
		this.context.getBean(LoggersEndpoint.class).setEnabled(false);
		this.mvc.perform(get("/loggers")).andExpect(status().isNotFound());
	}

	@Test
	public void getLoggerShouldReturnLogLevels() throws Exception {
		given(this.loggingSystem.getLoggerConfiguration("ROOT"))
				.willReturn(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG));
		this.mvc.perform(get("/loggers/ROOT")).andExpect(status().isOk())
				.andExpect(content().string(equalTo(
						"{\"configuredLevel\":null," + "\"effectiveLevel\":\"DEBUG\"}")));
	}

	@Test
	public void getLoggersRootWhenDisabledShouldReturnNotFound() throws Exception {
		this.context.getBean(LoggersEndpoint.class).setEnabled(false);
		this.mvc.perform(get("/loggers/ROOT")).andExpect(status().isNotFound());
	}

	@Test
	public void getLoggersWhenLoggerNotFoundShouldReturnNotFound() throws Exception {
		this.mvc.perform(get("/loggers/com.does.not.exist"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void contentTypeForGetDefaultsToActuatorV1Json() throws Exception {
		this.mvc.perform(get("/loggers")).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.spring-boot.actuator.v1+json;charset=UTF-8"));
	}

	@Test
	public void contentTypeForGetCanBeApplicationJson() throws Exception {
		this.mvc.perform(get("/loggers").header(HttpHeaders.ACCEPT,
				MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						MediaType.APPLICATION_JSON_UTF8_VALUE));
	}

	@Test
	public void setLoggerUsingApplicationJsonShouldSetLogLevel() throws Exception {
		this.mvc.perform(post("/loggers/ROOT").contentType(MediaType.APPLICATION_JSON)
				.content("{\"configuredLevel\":\"debug\"}")).andExpect(status().isOk());
		verify(this.loggingSystem).setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@Test
	public void setLoggerUsingActuatorV1JsonShouldSetLogLevel() throws Exception {
		this.mvc.perform(post("/loggers/ROOT")
				.contentType(ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON)
				.content("{\"configuredLevel\":\"debug\"}")).andExpect(status().isOk());
		verify(this.loggingSystem).setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@Test
	public void setLoggerWhenDisabledShouldReturnNotFound() throws Exception {
		this.context.getBean(LoggersEndpoint.class).setEnabled(false);
		this.mvc.perform(post("/loggers/ROOT").contentType(MediaType.APPLICATION_JSON)
				.content("{\"configuredLevel\":\"DEBUG\"}"))
				.andExpect(status().isNotFound());
		verifyZeroInteractions(this.loggingSystem);
	}

	@Test
	public void setLoggerWithWrongLogLevel() throws Exception {
		this.mvc.perform(post("/loggers/ROOT").contentType(MediaType.APPLICATION_JSON)
				.content("{\"configuredLevel\":\"other\"}"))
				.andExpect(status().is4xxClientError())
				.andExpect(status().reason(is("No such log level")));
		verifyZeroInteractions(this.loggingSystem);
	}

	@Test
	public void logLevelForLoggerWithNameThatCouldBeMistakenForAPathExtension()
			throws Exception {
		given(this.loggingSystem.getLoggerConfiguration("com.png"))
				.willReturn(new LoggerConfiguration("com.png", null, LogLevel.DEBUG));
		this.mvc.perform(get("/loggers/com.png")).andExpect(status().isOk())
				.andExpect(content().string(equalTo(
						"{\"configuredLevel\":null," + "\"effectiveLevel\":\"DEBUG\"}")));
	}

	@Configuration
	@Import({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	public static class TestConfiguration {

		@Bean
		public LoggingSystem loggingSystem() {
			return mock(LoggingSystem.class);
		}

		@Bean
		public LoggersEndpoint endpoint(LoggingSystem loggingSystem) {
			return new LoggersEndpoint(loggingSystem);
		}

	}

}
