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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.logging.LoggersEndpoint.GroupLoggerLevelsDescriptor;
import org.springframework.boot.actuate.logging.LoggersEndpoint.SingleLoggerLevelsDescriptor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggerConfiguration.ConfigurationScope;
import org.springframework.boot.logging.LoggerConfiguration.LevelConfiguration;
import org.springframework.boot.logging.LoggerGroup;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose a collection of {@link LoggerConfiguration}s.
 *
 * @author Ben Hale
 * @author Phillip Webb
 * @author HaiTao Zhang
 * @since 2.0.0
 */
@Endpoint(id = "loggers")
@RegisterReflectionForBinding({ GroupLoggerLevelsDescriptor.class, SingleLoggerLevelsDescriptor.class })
public class LoggersEndpoint {

	private final LoggingSystem loggingSystem;

	private final LoggerGroups loggerGroups;

	/**
	 * Create a new {@link LoggersEndpoint} instance.
	 * @param loggingSystem the logging system to expose
	 * @param loggerGroups the logger group to expose
	 */
	public LoggersEndpoint(LoggingSystem loggingSystem, LoggerGroups loggerGroups) {
		Assert.notNull(loggingSystem, "LoggingSystem must not be null");
		Assert.notNull(loggerGroups, "LoggerGroups must not be null");
		this.loggingSystem = loggingSystem;
		this.loggerGroups = loggerGroups;
	}

	@ReadOperation
	public LoggersDescriptor loggers() {
		Collection<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
		if (configurations == null) {
			return LoggersDescriptor.NONE;
		}
		return new LoggersDescriptor(getLevels(), getLoggers(configurations), getGroups());
	}

	private Map<String, GroupLoggerLevelsDescriptor> getGroups() {
		Map<String, GroupLoggerLevelsDescriptor> groups = new LinkedHashMap<>();
		this.loggerGroups.forEach((group) -> groups.put(group.getName(),
				new GroupLoggerLevelsDescriptor(group.getConfiguredLevel(), group.getMembers())));
		return groups;
	}

	@ReadOperation
	public LoggerLevelsDescriptor loggerLevels(@Selector String name) {
		Assert.notNull(name, "Name must not be null");
		LoggerGroup group = this.loggerGroups.get(name);
		if (group != null) {
			return new GroupLoggerLevelsDescriptor(group.getConfiguredLevel(), group.getMembers());
		}
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(name);
		return (configuration != null) ? new SingleLoggerLevelsDescriptor(configuration) : null;
	}

	@WriteOperation
	public void configureLogLevel(@Selector String name, @Nullable LogLevel configuredLevel) {
		Assert.notNull(name, "Name must not be empty");
		LoggerGroup group = this.loggerGroups.get(name);
		if (group != null && group.hasMembers()) {
			group.configureLogLevel(configuredLevel, this.loggingSystem::setLogLevel);
			return;
		}
		this.loggingSystem.setLogLevel(name, configuredLevel);
	}

	private NavigableSet<LogLevel> getLevels() {
		Set<LogLevel> levels = this.loggingSystem.getSupportedLogLevels();
		return new TreeSet<>(levels).descendingSet();
	}

	private Map<String, LoggerLevelsDescriptor> getLoggers(Collection<LoggerConfiguration> configurations) {
		Map<String, LoggerLevelsDescriptor> loggers = new LinkedHashMap<>(configurations.size());
		for (LoggerConfiguration configuration : configurations) {
			loggers.put(configuration.getName(), new SingleLoggerLevelsDescriptor(configuration));
		}
		return loggers;
	}

	/**
	 * Description of loggers.
	 */
	public static class LoggersDescriptor implements OperationResponseBody {

		/**
		 * Empty description.
		 */
		public static final LoggersDescriptor NONE = new LoggersDescriptor(null, null, null);

		private final NavigableSet<LogLevel> levels;

		private final Map<String, LoggerLevelsDescriptor> loggers;

		private final Map<String, GroupLoggerLevelsDescriptor> groups;

		public LoggersDescriptor(NavigableSet<LogLevel> levels, Map<String, LoggerLevelsDescriptor> loggers,
				Map<String, GroupLoggerLevelsDescriptor> groups) {
			this.levels = levels;
			this.loggers = loggers;
			this.groups = groups;
		}

		public NavigableSet<LogLevel> getLevels() {
			return this.levels;
		}

		public Map<String, LoggerLevelsDescriptor> getLoggers() {
			return this.loggers;
		}

		public Map<String, GroupLoggerLevelsDescriptor> getGroups() {
			return this.groups;
		}

	}

	/**
	 * Description of levels configured for a given logger.
	 */
	public static class LoggerLevelsDescriptor implements OperationResponseBody {

		private final String configuredLevel;

		public LoggerLevelsDescriptor(LogLevel configuredLevel) {
			this.configuredLevel = (configuredLevel != null) ? configuredLevel.name() : null;
		}

		LoggerLevelsDescriptor(LevelConfiguration directConfiguration) {
			this.configuredLevel = (directConfiguration != null) ? directConfiguration.getName() : null;
		}

		protected final String getName(LogLevel level) {
			return (level != null) ? level.name() : null;
		}

		public String getConfiguredLevel() {
			return this.configuredLevel;
		}

	}

	/**
	 * Description of levels configured for a given group logger.
	 */
	public static class GroupLoggerLevelsDescriptor extends LoggerLevelsDescriptor {

		private final List<String> members;

		public GroupLoggerLevelsDescriptor(LogLevel configuredLevel, List<String> members) {
			super(configuredLevel);
			this.members = members;
		}

		public List<String> getMembers() {
			return this.members;
		}

	}

	/**
	 * Description of levels configured for a given single logger.
	 */
	public static class SingleLoggerLevelsDescriptor extends LoggerLevelsDescriptor {

		private final String effectiveLevel;

		public SingleLoggerLevelsDescriptor(LoggerConfiguration configuration) {
			super(configuration.getLevelConfiguration(ConfigurationScope.DIRECT));
			this.effectiveLevel = configuration.getLevelConfiguration().getName();
		}

		public String getEffectiveLevel() {
			return this.effectiveLevel;
		}

	}

}
