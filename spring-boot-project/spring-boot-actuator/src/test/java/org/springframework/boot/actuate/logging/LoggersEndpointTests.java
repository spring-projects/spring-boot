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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.logging.LoggersEndpoint.GroupLoggerLevels;
import org.springframework.boot.actuate.logging.LoggersEndpoint.LoggerLevels;
import org.springframework.boot.actuate.logging.LoggersEndpoint.SingleLoggerLevels;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingGroups;
import org.springframework.boot.logging.LoggingSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggersEndpoint}.
 *
 * @author Ben Hale
 * @author Andy Wilkinson
 * @author HaiTao Zhang
 */
class LoggersEndpointTests {

	private final LoggingSystem loggingSystem = mock(LoggingSystem.class);

	private final LoggingGroups loggingGroups = mock(LoggingGroups.class);

	@Test
	@SuppressWarnings("unchecked")
	void loggersShouldReturnLoggerConfigurationsWithNoLoggerGroups() {
		given(this.loggingSystem.getLoggerConfigurations())
				.willReturn(Collections.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		given(this.loggingSystem.getSupportedLogLevels()).willReturn(EnumSet.allOf(LogLevel.class));
		given(this.loggingGroups.getLoggerGroupNames()).willReturn(null);
		Map<String, Object> result = new LoggersEndpoint(this.loggingSystem, this.loggingGroups).loggers();
		Map<String, LoggerLevels> loggers = (Map<String, LoggerLevels>) result.get("loggers");
		Set<LogLevel> levels = (Set<LogLevel>) result.get("levels");
		SingleLoggerLevels rootLevels = (SingleLoggerLevels) loggers.get("ROOT");
		assertThat(rootLevels.getConfiguredLevel()).isNull();
		assertThat(rootLevels.getEffectiveLevel()).isEqualTo("DEBUG");
		assertThat(levels).containsExactly(LogLevel.OFF, LogLevel.FATAL, LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO,
				LogLevel.DEBUG, LogLevel.TRACE);
		assertThat(result.get("groups")).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	void loggersShouldReturnLoggerConfigurationsWithLoggerGroups() {
		given(this.loggingSystem.getLoggerConfigurations())
				.willReturn(Collections.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		given(this.loggingSystem.getSupportedLogLevels()).willReturn(EnumSet.allOf(LogLevel.class));
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Collections.singletonList("test.member"));
		given(this.loggingGroups.getLoggerGroupNames()).willReturn(Collections.singleton("test"));
		given(this.loggingGroups.getLoggerGroupConfiguredLevel("test")).willReturn(LogLevel.DEBUG);
		Map<String, Object> result = new LoggersEndpoint(this.loggingSystem, this.loggingGroups).loggers();
		Map<String, LoggerLevels> loggerGroups = (Map<String, LoggerLevels>) result.get("groups");
		GroupLoggerLevels testLoggerLevel = (GroupLoggerLevels) loggerGroups.get("test");
		Map<String, LoggerLevels> loggers = (Map<String, LoggerLevels>) result.get("loggers");
		Set<LogLevel> levels = (Set<LogLevel>) result.get("levels");
		SingleLoggerLevels rootLevels = (SingleLoggerLevels) loggers.get("ROOT");
		assertThat(rootLevels.getConfiguredLevel()).isNull();
		assertThat(rootLevels.getEffectiveLevel()).isEqualTo("DEBUG");
		assertThat(levels).containsExactly(LogLevel.OFF, LogLevel.FATAL, LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO,
				LogLevel.DEBUG, LogLevel.TRACE);
		assertThat(loggerGroups).isNotNull();
		assertThat(testLoggerLevel).isNotNull();
		assertThat(testLoggerLevel.getConfiguredLevel()).isEqualTo("DEBUG");
		assertThat(testLoggerLevel.getMembers()).isEqualTo(Collections.singletonList("test.member"));
	}

	@Test
	void loggerLevelsWhenNameSpecifiedShouldReturnLevels() {
		given(this.loggingGroups.isGroup("ROOT")).willReturn(false);
		given(this.loggingSystem.getLoggerConfiguration("ROOT"))
				.willReturn(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG));
		SingleLoggerLevels levels = (SingleLoggerLevels) new LoggersEndpoint(this.loggingSystem, this.loggingGroups)
				.loggerLevels("ROOT");
		assertThat(levels.getConfiguredLevel()).isNull();
		assertThat(levels.getEffectiveLevel()).isEqualTo("DEBUG");
	}

	@Test
	void groupNameSpecifiedShouldReturnConfiguredLevelAndMembers() {
		given(this.loggingGroups.isGroup("test")).willReturn(true);
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Collections.singletonList("test.member"));
		given(this.loggingGroups.getLoggerGroupConfiguredLevel("test")).willReturn(LogLevel.DEBUG);
		GroupLoggerLevels levels = (GroupLoggerLevels) new LoggersEndpoint(this.loggingSystem, this.loggingGroups)
				.loggerLevels("test");
		assertThat(levels.getConfiguredLevel()).isEqualTo("DEBUG");
		assertThat(levels.getMembers()).isEqualTo(Collections.singletonList("test.member"));
	}

	@Test
	void configureLogLevelShouldSetLevelOnLoggingSystem() {
		given(this.loggingGroups.getLoggerGroup("ROOT")).willReturn(null);
		new LoggersEndpoint(this.loggingSystem, this.loggingGroups).configureLogLevel("ROOT", LogLevel.DEBUG);
		verify(this.loggingSystem).setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@Test
	void configureLogLevelWithNullSetsLevelOnLoggingSystemToNull() {
		given(this.loggingGroups.getLoggerGroup("ROOT")).willReturn(null);
		new LoggersEndpoint(this.loggingSystem, this.loggingGroups).configureLogLevel("ROOT", null);
		verify(this.loggingSystem).setLogLevel("ROOT", null);
	}

	@Test
	void configureLogLevelInLoggerGroupShouldSetLevelOnLoggingSystem() {
		given(this.loggingGroups.isGroup("test")).willReturn(true);
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Collections.singletonList("test.member"));
		new LoggersEndpoint(this.loggingSystem, this.loggingGroups).configureLogLevel("test", LogLevel.DEBUG);
		verify(this.loggingGroups).setLoggerGroupLevel("test", LogLevel.DEBUG);
	}

	@Test
	void configureLogLevelWithNullInLoggerGroupShouldSetLevelOnLoggingSystem() {
		given(this.loggingGroups.isGroup("test")).willReturn(true);
		given(this.loggingGroups.getLoggerGroup("test")).willReturn(Collections.singletonList("test.member"));
		new LoggersEndpoint(this.loggingSystem, this.loggingGroups).configureLogLevel("test", null);
		verify(this.loggingGroups).setLoggerGroupLevel("test", null);
	}

	// @Test
	// void

}
