/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.logging.LoggersEndpoint.GroupLoggerLevelsDescriptor;
import org.springframework.boot.actuate.logging.LoggersEndpoint.LoggerLevelsDescriptor;
import org.springframework.boot.actuate.logging.LoggersEndpoint.LoggersDescriptor;
import org.springframework.boot.actuate.logging.LoggersEndpoint.SingleLoggerLevelsDescriptor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggerConfiguration.LevelConfiguration;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LoggersEndpoint}.
 *
 * @author Ben Hale
 * @author Andy Wilkinson
 * @author HaiTao Zhang
 * @author Madhura Bhave
 */
class LoggersEndpointTests {

	private final LoggingSystem loggingSystem = mock(LoggingSystem.class);

	private LoggerGroups loggerGroups;

	@BeforeEach
	void setup() {
		Map<String, List<String>> groups = Collections.singletonMap("test", Collections.singletonList("test.member"));
		this.loggerGroups = new LoggerGroups(groups);
		this.loggerGroups.get("test").configureLogLevel(LogLevel.DEBUG, (a, b) -> {
		});
	}

	@Test
	void loggersShouldReturnLoggerConfigurationsWithNoLoggerGroups() {
		given(this.loggingSystem.getLoggerConfigurations())
			.willReturn(Collections.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		given(this.loggingSystem.getSupportedLogLevels()).willReturn(EnumSet.allOf(LogLevel.class));
		LoggersDescriptor result = new LoggersEndpoint(this.loggingSystem, new LoggerGroups()).loggers();
		Map<String, LoggerLevelsDescriptor> loggers = result.getLoggers();
		Set<LogLevel> levels = result.getLevels();
		SingleLoggerLevelsDescriptor rootLevels = (SingleLoggerLevelsDescriptor) loggers.get("ROOT");
		assertThat(rootLevels.getConfiguredLevel()).isNull();
		assertThat(rootLevels.getEffectiveLevel()).isEqualTo("DEBUG");
		assertThat(levels).containsExactly(LogLevel.OFF, LogLevel.FATAL, LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO,
				LogLevel.DEBUG, LogLevel.TRACE);
		Map<String, GroupLoggerLevelsDescriptor> groups = result.getGroups();
		assertThat(groups).isEmpty();
	}

	@Test
	void loggersShouldReturnLoggerConfigurationsWithLoggerGroups() {
		given(this.loggingSystem.getLoggerConfigurations())
			.willReturn(Collections.singletonList(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG)));
		given(this.loggingSystem.getSupportedLogLevels()).willReturn(EnumSet.allOf(LogLevel.class));
		LoggersDescriptor result = new LoggersEndpoint(this.loggingSystem, this.loggerGroups).loggers();
		Map<String, GroupLoggerLevelsDescriptor> loggerGroups = result.getGroups();
		GroupLoggerLevelsDescriptor groupLevel = loggerGroups.get("test");
		Map<String, LoggerLevelsDescriptor> loggers = result.getLoggers();
		Set<LogLevel> levels = result.getLevels();
		SingleLoggerLevelsDescriptor rootLevels = (SingleLoggerLevelsDescriptor) loggers.get("ROOT");
		assertThat(rootLevels.getConfiguredLevel()).isNull();
		assertThat(rootLevels.getEffectiveLevel()).isEqualTo("DEBUG");
		assertThat(levels).containsExactly(LogLevel.OFF, LogLevel.FATAL, LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO,
				LogLevel.DEBUG, LogLevel.TRACE);
		assertThat(loggerGroups).isNotNull();
		assertThat(groupLevel.getConfiguredLevel()).isEqualTo("DEBUG");
		assertThat(groupLevel.getMembers()).containsExactly("test.member");
	}

	@Test
	void loggerLevelsWhenNameSpecifiedShouldReturnLevels() {
		given(this.loggingSystem.getLoggerConfiguration("ROOT"))
			.willReturn(new LoggerConfiguration("ROOT", null, LogLevel.DEBUG));
		SingleLoggerLevelsDescriptor levels = (SingleLoggerLevelsDescriptor) new LoggersEndpoint(this.loggingSystem,
				this.loggerGroups)
			.loggerLevels("ROOT");
		assertThat(levels.getConfiguredLevel()).isNull();
		assertThat(levels.getEffectiveLevel()).isEqualTo("DEBUG");
	}

	@Test // gh-35227
	void loggerLevelsWhenCustomLevelShouldReturnLevels() {
		given(this.loggingSystem.getLoggerConfiguration("ROOT"))
			.willReturn(new LoggerConfiguration("ROOT", null, LevelConfiguration.ofCustom("FINEST")));
		SingleLoggerLevelsDescriptor levels = (SingleLoggerLevelsDescriptor) new LoggersEndpoint(this.loggingSystem,
				this.loggerGroups)
			.loggerLevels("ROOT");
		assertThat(levels.getConfiguredLevel()).isNull();
		assertThat(levels.getEffectiveLevel()).isEqualTo("FINEST");
	}

	@Test
	void groupNameSpecifiedShouldReturnConfiguredLevelAndMembers() {
		GroupLoggerLevelsDescriptor levels = (GroupLoggerLevelsDescriptor) new LoggersEndpoint(this.loggingSystem,
				this.loggerGroups)
			.loggerLevels("test");
		assertThat(levels.getConfiguredLevel()).isEqualTo("DEBUG");
		assertThat(levels.getMembers()).isEqualTo(Collections.singletonList("test.member"));
	}

	@Test
	void configureLogLevelShouldSetLevelOnLoggingSystem() {
		new LoggersEndpoint(this.loggingSystem, this.loggerGroups).configureLogLevel("ROOT", LogLevel.DEBUG);
		then(this.loggingSystem).should().setLogLevel("ROOT", LogLevel.DEBUG);
	}

	@Test
	void configureLogLevelWithNullSetsLevelOnLoggingSystemToNull() {
		new LoggersEndpoint(this.loggingSystem, this.loggerGroups).configureLogLevel("ROOT", null);
		then(this.loggingSystem).should().setLogLevel("ROOT", null);
	}

	@Test
	void configureLogLevelInLoggerGroupShouldSetLevelOnLoggingSystem() {
		new LoggersEndpoint(this.loggingSystem, this.loggerGroups).configureLogLevel("test", LogLevel.DEBUG);
		then(this.loggingSystem).should().setLogLevel("test.member", LogLevel.DEBUG);
	}

	@Test
	void configureLogLevelWithNullInLoggerGroupShouldSetLevelOnLoggingSystem() {
		new LoggersEndpoint(this.loggingSystem, this.loggerGroups).configureLogLevel("test", null);
		then(this.loggingSystem).should().setLogLevel("test.member", null);
	}

	@Test
	void registersRuntimeHintsForClassesSerializedToJson() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new ReflectiveRuntimeHintsRegistrar().registerRuntimeHints(runtimeHints, LoggersEndpoint.class);
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		assertThat(reflection.onType(LoggerLevelsDescriptor.class)).accepts(runtimeHints);
		assertThat(reflection.onMethod(LoggerLevelsDescriptor.class, "getConfiguredLevel")).accepts(runtimeHints);
		assertThat(reflection.onType(SingleLoggerLevelsDescriptor.class)).accepts(runtimeHints);
		assertThat(reflection.onMethod(SingleLoggerLevelsDescriptor.class, "getEffectiveLevel")).accepts(runtimeHints);
		assertThat(reflection.onMethod(SingleLoggerLevelsDescriptor.class, "getConfiguredLevel")).accepts(runtimeHints);
		assertThat(reflection.onType(GroupLoggerLevelsDescriptor.class)).accepts(runtimeHints);
		assertThat(reflection.onMethod(GroupLoggerLevelsDescriptor.class, "getMembers")).accepts(runtimeHints);
		assertThat(reflection.onMethod(GroupLoggerLevelsDescriptor.class, "getConfiguredLevel")).accepts(runtimeHints);
	}

}
