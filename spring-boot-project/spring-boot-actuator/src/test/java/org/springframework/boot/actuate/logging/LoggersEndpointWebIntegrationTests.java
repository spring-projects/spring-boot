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
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingGroups;
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
 * @author HaiTao Zhang
 */
class LoggersEndpointWebIntegrationTests {

	private WebTestClient client;

	private LoggingSystem loggingSystem;

	private LoggingGroups loggingGroups;

	private ObjectProvider<LoggingGroups> loggingGroupsObjectProvider;

	@BeforeEach
	@AfterEach
	void resetMocks(ConfigurableApplicationContext context, WebTestClient client) {
		this.client = client;
		this.loggingSystem = context.getBean(LoggingSystem.class);
		this.loggingGroups = context.getBean(LoggingGroups.class);
		this.loggingGroupsObjectProvider = context.getBean(ObjectProvider.class);
		Mockito.reset(this.loggingSystem);
		Mockito.reset(this.loggingGroups);
		given(this.loggingSystem.getSupportedLogLevels()).willReturn(EnumSet.allOf(LogLevel.class));
		given(this.loggingGroupsObjectProvider.getIfAvailable()).willReturn(this.loggingGroups);
	}

	@WebEndpointTest
	void getLoggerShouldReturnAllLoggerConfigurations() {
		given(this.loggingGroups.getLoggerGroupNames()).willReturn(null);
		given(this.loggingSystem.getLoggerConfigurations())
				.willReturn(Collections.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		this.client.get().uri("/actuator/loggers").exchange().expectStatus().isOk().expectBody().jsonPath("$.length()")
				.isEqualTo(2).jsonPath("levels")
				.isEqualTo(jsonArrayOf("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"))
				.jsonPath("loggers.length()").isEqualTo(1).jsonPath("loggers.ROOT.length()").isEqualTo(2)
				.jsonPath("loggers.ROOT.configuredLevel").isEqualTo(null).jsonPath("loggers.ROOT.effectiveLevel")
				.isEqualTo("DEBUG");
	}

	@WebEndpointTest
	void getLoggerShouldReturnAllLoggerConfigurationsWithLoggerGroups() {
		given(this.loggingGroups.getLoggerGroupNames()).willReturn(Collections.singleton("test"));
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Arrays.asList("test.member1", "test.member2"));
		given(this.loggingGroups.getLoggerGroupConfiguredLevel("test")).willReturn(LogLevel.DEBUG);
		given(this.loggingSystem.getLoggerConfigurations())
				.willReturn(Collections.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		this.client.get().uri("/actuator/loggers").exchange().expectStatus().isOk().expectBody().jsonPath("$.length()")
				.isEqualTo(3).jsonPath("levels")
				.isEqualTo(jsonArrayOf("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"))
				.jsonPath("loggers.length()").isEqualTo(1).jsonPath("loggers.ROOT.length()").isEqualTo(2)
				.jsonPath("loggers.ROOT.configuredLevel").isEqualTo(null).jsonPath("loggers.ROOT.effectiveLevel")
				.isEqualTo("DEBUG").jsonPath("groups.length()").isEqualTo(1).jsonPath("groups.test.length()")
				.isEqualTo(2).jsonPath("groups.test.configuredLevel").isEqualTo("DEBUG")
				.jsonPath("groups.test.members.length()").isEqualTo(2).jsonPath("groups.test.members")
				.value(IsIterableContainingInAnyOrder.containsInAnyOrder("test.member1", "test.member2"));
	}

	@WebEndpointTest
	void getLoggerShouldReturnLogLevels() {
		given(this.loggingGroups.isGroup("ROOT")).willReturn(false);
		given(this.loggingSystem.getLoggerConfiguration("ROOT"))
				.willReturn(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG));
		this.client.get().uri("/actuator/loggers/ROOT").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.length()").isEqualTo(2).jsonPath("configuredLevel").isEqualTo(null)
				.jsonPath("effectiveLevel").isEqualTo("DEBUG");
	}

	@WebEndpointTest
	void getLoggersWhenLoggerAndLoggerGroupNotFoundShouldReturnNotFound() {
		this.client.get().uri("/actuator/loggers/com.does.not.exist").exchange().expectStatus().isNotFound();
	}

	@WebEndpointTest
	void getLoggerGroupShouldReturnConfiguredLogLevelAndMembers() {
		given(this.loggingGroups.isGroup("test")).willReturn(true);
		given(this.loggingGroups.getLoggerGroupConfiguredLevel("test")).willReturn(LogLevel.DEBUG);
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Arrays.asList("test.member1", "test.member2"));
		this.client.get().uri("actuator/loggers/test").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.length()").isEqualTo(2).jsonPath("members")
				.value(IsIterableContainingInAnyOrder.containsInAnyOrder("test.member1", "test.member2"))
				.jsonPath("configuredLevel").isEqualTo("DEBUG");
	}

	@WebEndpointTest
	void setLoggerUsingApplicationJsonShouldSetLogLevel() {
		given(this.loggingGroups.isGroup("ROOT")).willReturn(false);
		this.client.post().uri("/actuator/loggers/ROOT").contentType(MediaType.APPLICATION_JSON)
				.body(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus().isNoContent();
		verify(this.loggingSystem).setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerUsingActuatorV2JsonShouldSetLogLevel() {
		given(this.loggingGroups.isGroup("ROOT")).willReturn(false);
		this.client.post().uri("/actuator/loggers/ROOT")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON))
				.body(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus().isNoContent();
		verify(this.loggingSystem).setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerGroupUsingActuatorV2JsonShouldSetLogLevel() {
		given(this.loggingGroups.isGroup("test")).willReturn(true);
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Arrays.asList("test.member1", "test.member2"));
		this.client.post().uri("/actuator/loggers/test")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON))
				.body(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus().isNoContent();
		verify(this.loggingGroups).setLoggerGroupLevel("test", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerGroupUsingApplicationJsonShouldSetLogLevel() {
		given(this.loggingGroups.isGroup("test")).willReturn(true);
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Arrays.asList("test.member1", "test.member2"));
		this.client.post().uri("/actuator/loggers/test").contentType(MediaType.APPLICATION_JSON)
				.body(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus().isNoContent();
		verify(this.loggingGroups).setLoggerGroupLevel("test", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerOrLoggerGroupWithWrongLogLevelResultInBadRequestResponse() {
		this.client.post().uri("/actuator/loggers/ROOT").contentType(MediaType.APPLICATION_JSON)
				.body(Collections.singletonMap("configuredLevel", "other")).exchange().expectStatus().isBadRequest();
		verifyZeroInteractions(this.loggingSystem);
	}

	@WebEndpointTest
	void setLoggerWithNullLogLevel() {
		given(this.loggingGroups.isGroup("ROOT")).willReturn(false);
		this.client.post().uri("/actuator/loggers/ROOT")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON))
				.body(Collections.singletonMap("configuredLevel", null)).exchange().expectStatus().isNoContent();
		verify(this.loggingSystem).setLogLevel("ROOT", null);
	}

	@WebEndpointTest
	void setLoggerWithNoLogLevel() {
		given(this.loggingGroups.isGroup("ROOT")).willReturn(false);
		this.client.post().uri("/actuator/loggers/ROOT")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON)).body(Collections.emptyMap())
				.exchange().expectStatus().isNoContent();
		verify(this.loggingSystem).setLogLevel("ROOT", null);
	}

	@WebEndpointTest
	void setLoggerGroupWithNullLogLevel() {
		given(this.loggingGroups.isGroup("test")).willReturn(true);
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Arrays.asList("test.member1", "test.member2"));
		this.client.post().uri("/actuator/loggers/test")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON))
				.body(Collections.singletonMap("configuredLevel", null)).exchange().expectStatus().isNoContent();
		verify(this.loggingGroups).setLoggerGroupLevel("test", null);
	}

	@WebEndpointTest
	void setLoggerGroupWithNoLogLevel() {
		given(this.loggingGroups.isGroup("test")).willReturn(true);
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Arrays.asList("test.member1", "test.member2"));
		this.client.post().uri("/actuator/loggers/test")
				.contentType(MediaType.parseMediaType(ActuatorMediaType.V2_JSON)).body(Collections.emptyMap())
				.exchange().expectStatus().isNoContent();
		verify(this.loggingGroups).setLoggerGroupLevel("test", null);
	}

	@WebEndpointTest
	void logLevelForLoggerWithNameThatCouldBeMistakenForAPathExtension() {
		given(this.loggingSystem.getLoggerConfiguration("com.png"))
				.willReturn(new LoggerConfiguration("com.png", null, LogLevel.DEBUG));
		this.client.get().uri("/actuator/loggers/com.png").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.length()").isEqualTo(2).jsonPath("configuredLevel").isEqualTo(null)
				.jsonPath("effectiveLevel").isEqualTo("DEBUG");
	}

	@WebEndpointTest
	void logLevelForLoggerGroupWithNameThatCouldBeMistakenForAPathExtension() {
		given(this.loggingGroups.isGroup("com.png")).willReturn(true);
		given(this.loggingGroups.getLoggerGroupConfiguredLevel("com.png")).willReturn(LogLevel.DEBUG);
		given(this.loggingGroups.getLoggerGroup("com.png")).willReturn(Arrays.asList("test.member1", "test.member2"));
		this.client.get().uri("/actuator/loggers/com.png").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.length()").isEqualTo(2).jsonPath("configuredLevel").isEqualTo("DEBUG").jsonPath("members")
				.value(IsIterableContainingInAnyOrder.containsInAnyOrder("test.member1", "test.member2"));
	}

	private JSONArray jsonArrayOf(Object... entries) {
		JSONArray array = new JSONArray();
		array.addAll(Arrays.asList(entries));
		return array;
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		LoggingSystem loggingSystem() {
			return mock(LoggingSystem.class);
		}

		@Bean
		ObjectProvider<LoggingGroups> loggingGroupsObjectProvider() {
			return mock(ObjectProvider.class);
		}

		@Bean
		LoggingGroups loggingGroups() {
			return mock(LoggingGroups.class);
		}

		@Bean
		LoggersEndpoint endpoint(LoggingSystem loggingSystem,
				ObjectProvider<LoggingGroups> loggingGroupsObjectProvider) {
			return new LoggersEndpoint(loggingSystem, loggingGroupsObjectProvider.getIfAvailable());
		}

	}

}
