/*
 * Copyright 2016-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

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
	public void invoke() throws Exception {
		given(getLoggingSystem().listLoggerConfigurations()).willReturn(Collections
				.singleton(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		Map<String, String> loggingConfiguration = getEndpointBean().invoke()
				.get("ROOT");
		assertThat(loggingConfiguration.get("configuredLevel")).isNull();
		assertThat(loggingConfiguration.get("effectiveLevel")).isEqualTo("DEBUG");
	}

	public void get() throws Exception {
		given(getLoggingSystem().getLoggerConfiguration("ROOT"))
				.willReturn(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG));
		Map<String, String> loggingConfiguration = getEndpointBean().get("ROOT");
		assertThat(loggingConfiguration.get("configuredLevel")).isNull();
		assertThat(loggingConfiguration.get("effectiveLevel")).isEqualTo("DEBUG");
	}

	public void set() throws Exception {
		getEndpointBean().set("ROOT", LogLevel.DEBUG);
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
