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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.LoggersEndpoint.LoggerLevels;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggersEndpoint}.
 *
 * @author Ben Hale
 */
public class LoggersEndpointTests extends AbstractEndpointTests<LoggersEndpoint> {

	public LoggersEndpointTests() {
		super(Config.class, LoggersEndpoint.class, "loggers", true, "endpoints.loggers");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void invokeShouldReturnConfigurations() throws Exception {
		given(getLoggingSystem().getLoggerConfigurations()).willReturn(Collections
				.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		given(getLoggingSystem().getSupportedLogLevels())
				.willReturn(EnumSet.allOf(LogLevel.class));
		Map<String, Object> result = getEndpointBean().invoke();
		Map<String, LoggerLevels> loggers = (Map<String, LoggerLevels>) result
				.get("loggers");
		Set<LogLevel> levels = (Set<LogLevel>) result.get("levels");
		LoggerLevels rootLevels = loggers.get("ROOT");
		assertThat(rootLevels.getConfiguredLevel()).isNull();
		assertThat(rootLevels.getEffectiveLevel()).isEqualTo("DEBUG");
		assertThat(levels).containsExactly(LogLevel.OFF, LogLevel.FATAL, LogLevel.ERROR,
				LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG, LogLevel.TRACE);
	}

	@Test
	public void invokeWhenNameSpecifiedShouldReturnLevels() throws Exception {
		given(getLoggingSystem().getLoggerConfiguration("ROOT"))
				.willReturn(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG));
		LoggerLevels levels = getEndpointBean().invoke("ROOT");
		assertThat(levels.getConfiguredLevel()).isNull();
		assertThat(levels.getEffectiveLevel()).isEqualTo("DEBUG");
	}

	@Test
	public void setLogLevelShouldSetLevelOnLoggingSystem() throws Exception {
		getEndpointBean().setLogLevel("ROOT", LogLevel.DEBUG);
		verify(getLoggingSystem()).setLogLevel("ROOT", LogLevel.DEBUG);
	}

	private LoggingSystem getLoggingSystem() {
		return this.context.getBean(LoggingSystem.class);
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

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
