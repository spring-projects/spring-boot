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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose a collection of {@link LoggerConfiguration}s.
 *
 * @author Ben Hale
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "endpoints.loggers")
public class LoggersEndpoint extends AbstractEndpoint<Map<String, Map<String, String>>> {

	private final LoggingSystem loggingSystem;

	/**
	 * Create a new {@link LoggersEndpoint} instance.
	 * @param loggingSystem the logging system to expose
	 */
	public LoggersEndpoint(LoggingSystem loggingSystem) {
		super("loggers");
		Assert.notNull(loggingSystem, "LoggingSystem must not be null");
		this.loggingSystem = loggingSystem;
	}

	@Override
	public Map<String, Map<String, String>> invoke() {
		Collection<LoggerConfiguration> loggerConfigurations = this.loggingSystem
				.listLoggerConfigurations();

		if (loggerConfigurations == null) {
			return Collections.emptyMap();
		}

		Map<String, Map<String, String>> result = new LinkedHashMap<String,
				Map<String, String>>(loggerConfigurations.size());

		for (LoggerConfiguration loggerConfiguration : loggerConfigurations) {
			result.put(loggerConfiguration.getName(), result(loggerConfiguration));
		}

		return result;
	}

	public Map<String, String> get(String name) {
		Assert.notNull(name, "Name must not be null");
		return result(this.loggingSystem.getLoggerConfiguration(name));
	}

	public void set(String name, LogLevel level) {
		Assert.notNull(name, "Name must not be empty");
		this.loggingSystem.setLogLevel(name, level);
	}

	private static Map<String, String> result(LoggerConfiguration loggerConfiguration) {
		if (loggerConfiguration == null) {
			return Collections.emptyMap();
		}
		Map<String, String> result = new LinkedHashMap<String, String>(3);
		LogLevel configuredLevel = loggerConfiguration.getConfiguredLevel();
		result.put("configuredLevel",
				configuredLevel != null ? configuredLevel.name() : null);
		result.put("effectiveLevel", loggerConfiguration.getEffectiveLevel().name());
		return result;
	}

}
