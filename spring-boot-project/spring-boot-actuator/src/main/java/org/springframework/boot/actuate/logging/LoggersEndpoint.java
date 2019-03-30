/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose a collection of {@link LoggerConfiguration}s.
 *
 * @author Ben Hale
 * @author Phillip Webb
 * @since 2.0.0
 */
@Endpoint(id = "loggers")
public class LoggersEndpoint {

	private final LoggingSystem loggingSystem;

	/**
	 * Create a new {@link LoggersEndpoint} instance.
	 * @param loggingSystem the logging system to expose
	 */
	public LoggersEndpoint(LoggingSystem loggingSystem) {
		Assert.notNull(loggingSystem, "LoggingSystem must not be null");
		this.loggingSystem = loggingSystem;
	}

	@ReadOperation
	public Map<String, Object> loggers() {
		Collection<LoggerConfiguration> configurations = this.loggingSystem
				.getLoggerConfigurations();
		if (configurations == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("levels", getLevels());
		result.put("loggers", getLoggers(configurations));
		return result;
	}

	@ReadOperation
	public LoggerLevels loggerLevels(@Selector String name) {
		Assert.notNull(name, "Name must not be null");
		LoggerConfiguration configuration = this.loggingSystem
				.getLoggerConfiguration(name);
		return (configuration != null) ? new LoggerLevels(configuration) : null;
	}

	@WriteOperation
	public void configureLogLevel(@Selector String name,
			@Nullable LogLevel configuredLevel) {
		Assert.notNull(name, "Name must not be empty");
		this.loggingSystem.setLogLevel(name, configuredLevel);
	}

	private NavigableSet<LogLevel> getLevels() {
		Set<LogLevel> levels = this.loggingSystem.getSupportedLogLevels();
		return new TreeSet<>(levels).descendingSet();
	}

	private Map<String, LoggerLevels> getLoggers(
			Collection<LoggerConfiguration> configurations) {
		Map<String, LoggerLevels> loggers = new LinkedHashMap<>(configurations.size());
		for (LoggerConfiguration configuration : configurations) {
			loggers.put(configuration.getName(), new LoggerLevels(configuration));
		}
		return loggers;
	}

	/**
	 * Levels configured for a given logger exposed in a JSON friendly way.
	 */
	public static class LoggerLevels {

		private String configuredLevel;

		private String effectiveLevel;

		public LoggerLevels(LoggerConfiguration configuration) {
			this.configuredLevel = getName(configuration.getConfiguredLevel());
			this.effectiveLevel = getName(configuration.getEffectiveLevel());
		}

		private String getName(LogLevel level) {
			return (level != null) ? level.name() : null;
		}

		public String getConfiguredLevel() {
			return this.configuredLevel;
		}

		public String getEffectiveLevel() {
			return this.effectiveLevel;
		}

	}

}
