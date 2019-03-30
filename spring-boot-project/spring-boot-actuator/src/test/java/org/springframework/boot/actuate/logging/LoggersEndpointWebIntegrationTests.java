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

package org.springframework.boot.actuate.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import net.minidev.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointRunners;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Integration tests for {@link LoggersEndpoint} when exposed via Jersey, Spring MVC, and
 * WebFlux.
 *
 * @author Ben Hale
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@RunWith(WebEndpointRunners.class)
public class LoggersEndpointWebIntegrationTests {

	private static ConfigurableApplicationContext context;

	private static WebTestClient client;

	private LoggingSystem loggingSystem;

	@Before
	@After
	public void resetMocks() {
		this.loggingSystem = context.getBean(LoggingSystem.class);
		Mockito.reset(this.loggingSystem);
		given(this.loggingSystem.getSupportedLogLevels())
				.willReturn(EnumSet.allOf(LogLevel.class));
	}

	@Test
	public void getLoggerShouldReturnAllLoggerConfigurations() {
		given(this.loggingSystem.getLoggerConfigurations()).willReturn(Collections
				.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		client.get().uri("/actuator/loggers").exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(2).jsonPath("levels")
				.isEqualTo(jsonArrayOf("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG",
						"TRACE"))
				.jsonPath("loggers.length()").isEqualTo(1)
				.jsonPath("loggers.ROOT.length()").isEqualTo(2)
				.jsonPath("loggers.ROOT.configuredLevel").isEqualTo(null)
				.jsonPath("loggers.ROOT.effectiveLevel").isEqualTo("DEBUG");
	}

	@Test
	public void getLoggerShouldReturnLogLevels() {
		given(this.loggingSystem.getLoggerConfiguration("ROOT"))
				.willReturn(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG));
		client.get().uri("/actuator/loggers/ROOT").exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(2)
				.jsonPath("configuredLevel").isEqualTo(null).jsonPath("effectiveLevel")
				.isEqualTo("DEBUG");
	}

	@Test
	public void getLoggersWhenLoggerNotFoundShouldReturnNotFound() {
		client.get().uri("/actuator/loggers/com.does.not.exist").exchange().expectStatus()
				.isNotFound();
	}

	@Test
	public void setLoggerUsingApplicationJsonShouldSetLogLevel() {
		client.post().uri("/actuator/loggers/ROOT")
				.contentType(MediaType.APPLICATION_JSON)
				.syncBody(Collections.singletonMap("configuredLevel", "debug")).exchange()
				.expectStatus().isNoContent();
		verify(this.loggingSystem).setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@Test
	public void setLoggerUsingActuatorV2JsonShouldSetLogLevel() {
		client.post().uri("/actuator/loggers/ROOT")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON))
				.syncBody(Collections.singletonMap("configuredLevel", "debug")).exchange()
				.expectStatus().isNoContent();
		verify(this.loggingSystem).setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@Test
	public void setLoggerWithWrongLogLevelResultInBadRequestResponse() {
		client.post().uri("/actuator/loggers/ROOT")
				.contentType(MediaType.APPLICATION_JSON)
				.syncBody(Collections.singletonMap("configuredLevel", "other")).exchange()
				.expectStatus().isBadRequest();
		verifyZeroInteractions(this.loggingSystem);
	}

	@Test
	public void setLoggerWithNullLogLevel() {
		client.post().uri("/actuator/loggers/ROOT")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON))
				.syncBody(Collections.singletonMap("configuredLevel", null)).exchange()
				.expectStatus().isNoContent();
		verify(this.loggingSystem).setLogLevel("ROOT", null);
	}

	@Test
	public void setLoggerWithNoLogLevel() {
		client.post().uri("/actuator/loggers/ROOT")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON))
				.syncBody(Collections.emptyMap()).exchange().expectStatus().isNoContent();
		verify(this.loggingSystem).setLogLevel("ROOT", null);
	}

	@Test
	public void logLevelForLoggerWithNameThatCouldBeMistakenForAPathExtension() {
		given(this.loggingSystem.getLoggerConfiguration("com.png"))
				.willReturn(new LoggerConfiguration("com.png", null, LogLevel.DEBUG));
		client.get().uri("/actuator/loggers/com.png").exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(2)
				.jsonPath("configuredLevel").isEqualTo(null).jsonPath("effectiveLevel")
				.isEqualTo("DEBUG");
	}

	private JSONArray jsonArrayOf(Object... entries) {
		JSONArray array = new JSONArray();
		array.addAll(Arrays.asList(entries));
		return array;
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

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
