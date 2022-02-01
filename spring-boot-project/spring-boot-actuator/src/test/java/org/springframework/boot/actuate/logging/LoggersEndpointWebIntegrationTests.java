/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

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
 * @author Madhura Bhave
 */
class LoggersEndpointWebIntegrationTests {

	private static final String V2_JSON = ApiVersion.V2.getProducedMimeType().toString();

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

	private WebTestClient client;

	private LoggingSystem loggingSystem;

	private LoggerGroups loggerGroups;

	@BeforeEach
	@AfterEach
	void resetMocks(ConfigurableApplicationContext context, WebTestClient client) {
		this.client = client;
		this.loggingSystem = context.getBean(LoggingSystem.class);
		this.loggerGroups = context.getBean(LoggerGroups.class);
		Mockito.reset(this.loggingSystem);
		given(this.loggingSystem.getSupportedLogLevels()).willReturn(EnumSet.allOf(LogLevel.class));
	}

	@WebEndpointTest
	void getLoggerShouldReturnAllLoggerConfigurationsWithLoggerGroups() {
		setLogLevelToDebug("test");
		given(this.loggingSystem.getLoggerConfigurations())
				.willReturn(Collections.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		this.client.get().uri("/actuator/loggers").exchange().expectStatus().isOk().expectBody().jsonPath("$.length()")
				.isEqualTo(3).jsonPath("levels")
				.isEqualTo(jsonArrayOf("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"))
				.jsonPath("loggers.length()").isEqualTo(1).jsonPath("loggers.ROOT.length()").isEqualTo(2)
				.jsonPath("loggers.ROOT.configuredLevel").isEqualTo(null).jsonPath("loggers.ROOT.effectiveLevel")
				.isEqualTo("DEBUG").jsonPath("groups.length()").isEqualTo(2).jsonPath("groups.test.configuredLevel")
				.isEqualTo("DEBUG");
	}

	@WebEndpointTest
	void getLoggerShouldReturnLogLevels() {
		setLogLevelToDebug("test");
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
		setLogLevelToDebug("test");
		this.client.get().uri("actuator/loggers/test").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.length()").isEqualTo(2).jsonPath("members")
				.value(IsIterableContainingInAnyOrder.containsInAnyOrder("test.member1", "test.member2"))
				.jsonPath("configuredLevel").isEqualTo("DEBUG");
	}

	@WebEndpointTest
	void setLoggerUsingApplicationJsonShouldSetLogLevel() {
		this.client.post().uri("/actuator/loggers/ROOT").contentType(MediaType.APPLICATION_JSON)
				.bodyValue(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus()
				.isNoContent();
		then(this.loggingSystem).should().setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerUsingActuatorV2JsonShouldSetLogLevel() {
		this.client.post().uri("/actuator/loggers/ROOT").contentType(MediaType.parseMediaType(V2_JSON))
				.bodyValue(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus()
				.isNoContent();
		then(this.loggingSystem).should().setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerUsingActuatorV3JsonShouldSetLogLevel() {
		this.client.post().uri("/actuator/loggers/ROOT").contentType(MediaType.parseMediaType(V3_JSON))
				.bodyValue(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus()
				.isNoContent();
		then(this.loggingSystem).should().setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerGroupUsingActuatorV2JsonShouldSetLogLevel() {
		this.client.post().uri("/actuator/loggers/test").contentType(MediaType.parseMediaType(V2_JSON))
				.bodyValue(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus()
				.isNoContent();
		then(this.loggingSystem).should().setLogLevel("test.member1", LogLevel.DEBUG);
		then(this.loggingSystem).should().setLogLevel("test.member2", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerGroupUsingApplicationJsonShouldSetLogLevel() {
		this.client.post().uri("/actuator/loggers/test").contentType(MediaType.APPLICATION_JSON)
				.bodyValue(Collections.singletonMap("configuredLevel", "debug")).exchange().expectStatus()
				.isNoContent();
		then(this.loggingSystem).should().setLogLevel("test.member1", LogLevel.DEBUG);
		then(this.loggingSystem).should().setLogLevel("test.member2", LogLevel.DEBUG);
	}

	@WebEndpointTest
	void setLoggerOrLoggerGroupWithWrongLogLevelResultInBadRequestResponse() {
		this.client.post().uri("/actuator/loggers/ROOT").contentType(MediaType.APPLICATION_JSON)
				.bodyValue(Collections.singletonMap("configuredLevel", "other")).exchange().expectStatus()
				.isBadRequest();
		then(this.loggingSystem).shouldHaveNoInteractions();
	}

	@WebEndpointTest
	void setLoggerWithNullLogLevel() {
		this.client.post().uri("/actuator/loggers/ROOT").contentType(MediaType.parseMediaType(V3_JSON))
				.bodyValue(Collections.singletonMap("configuredLevel", null)).exchange().expectStatus().isNoContent();
		then(this.loggingSystem).should().setLogLevel("ROOT", null);
	}

	@WebEndpointTest
	void setLoggerWithNoLogLevel() {
		this.client.post().uri("/actuator/loggers/ROOT").contentType(MediaType.parseMediaType(V3_JSON))
				.bodyValue(Collections.emptyMap()).exchange().expectStatus().isNoContent();
		then(this.loggingSystem).should().setLogLevel("ROOT", null);
	}

	@WebEndpointTest
	void setLoggerGroupWithNullLogLevel() {
		this.client.post().uri("/actuator/loggers/test").contentType(MediaType.parseMediaType(V3_JSON))
				.bodyValue(Collections.singletonMap("configuredLevel", null)).exchange().expectStatus().isNoContent();
		then(this.loggingSystem).should().setLogLevel("test.member1", null);
		then(this.loggingSystem).should().setLogLevel("test.member2", null);
	}

	@WebEndpointTest
	void setLoggerGroupWithNoLogLevel() {
		this.client.post().uri("/actuator/loggers/test").contentType(MediaType.parseMediaType(V3_JSON))
				.bodyValue(Collections.emptyMap()).exchange().expectStatus().isNoContent();
		then(this.loggingSystem).should().setLogLevel("test.member1", null);
		then(this.loggingSystem).should().setLogLevel("test.member2", null);
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
		setLogLevelToDebug("group.png");
		this.client.get().uri("/actuator/loggers/group.png").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.length()").isEqualTo(2).jsonPath("configuredLevel").isEqualTo("DEBUG").jsonPath("members")
				.value(IsIterableContainingInAnyOrder.containsInAnyOrder("png.member1", "png.member2"));
	}

	private void setLogLevelToDebug(String name) {
		this.loggerGroups.get(name).configureLogLevel(LogLevel.DEBUG, (a, b) -> {
		});
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
		LoggerGroups loggingGroups() {
			return getLoggerGroups();
		}

		private LoggerGroups getLoggerGroups() {
			Map<String, List<String>> groups = new LinkedHashMap<>();
			groups.put("test", Arrays.asList("test.member1", "test.member2"));
			groups.put("group.png", Arrays.asList("png.member1", "png.member2"));
			return new LoggerGroups(groups);
		}

		@Bean
		LoggersEndpoint endpoint(LoggingSystem loggingSystem,
				ObjectProvider<LoggerGroups> loggingGroupsObjectProvider) {
			return new LoggersEndpoint(loggingSystem, loggingGroupsObjectProvider.getIfAvailable());
		}

	}

}
