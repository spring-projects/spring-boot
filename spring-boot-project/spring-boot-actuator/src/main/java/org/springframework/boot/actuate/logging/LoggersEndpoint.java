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

	/**
	 * Retrieves the loggers descriptor.
	 * @return The loggers descriptor containing information about the loggers, levels,
	 * and groups.
	 */
	@ReadOperation
	public LoggersDescriptor loggers() {
		Collection<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
		if (configurations == null) {
			return LoggersDescriptor.NONE;
		}
		return new LoggersDescriptor(getLevels(), getLoggers(configurations), getGroups());
	}

	/**
	 * Retrieves the groups of logger levels and their corresponding descriptors.
	 * @return a map containing the groups of logger levels and their descriptors
	 */
	private Map<String, GroupLoggerLevelsDescriptor> getGroups() {
		Map<String, GroupLoggerLevelsDescriptor> groups = new LinkedHashMap<>();
		this.loggerGroups.forEach((group) -> groups.put(group.getName(),
				new GroupLoggerLevelsDescriptor(group.getConfiguredLevel(), group.getMembers())));
		return groups;
	}

	/**
	 * Retrieves the logger levels descriptor for the specified logger name.
	 * @param name the name of the logger
	 * @return the logger levels descriptor for the specified logger name, or null if not
	 * found
	 * @throws IllegalArgumentException if the name is null
	 */
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

	/**
	 * Configures the log level for a specific logger.
	 * @param name the name of the logger
	 * @param configuredLevel the log level to be configured
	 * @throws IllegalArgumentException if the name is empty
	 */
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

	/**
	 * Retrieves the supported log levels in descending order.
	 * @return a navigable set of log levels in descending order
	 */
	private NavigableSet<LogLevel> getLevels() {
		Set<LogLevel> levels = this.loggingSystem.getSupportedLogLevels();
		return new TreeSet<>(levels).descendingSet();
	}

	/**
	 * Retrieves the loggers and their levels based on the provided configurations.
	 * @param configurations the collection of logger configurations
	 * @return a map containing the loggers and their levels
	 */
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

		/**
		 * Constructs a new LoggersDescriptor with the specified levels, loggers, and
		 * groups.
		 * @param levels the NavigableSet of LogLevel objects representing the available
		 * log levels
		 * @param loggers the Map of String to LoggerLevelsDescriptor objects representing
		 * the loggers and their associated log levels
		 * @param groups the Map of String to GroupLoggerLevelsDescriptor objects
		 * representing the logger groups and their associated log levels
		 */
		public LoggersDescriptor(NavigableSet<LogLevel> levels, Map<String, LoggerLevelsDescriptor> loggers,
				Map<String, GroupLoggerLevelsDescriptor> groups) {
			this.levels = levels;
			this.loggers = loggers;
			this.groups = groups;
		}

		/**
		 * Returns the NavigableSet of LogLevel objects.
		 * @return the NavigableSet of LogLevel objects
		 */
		public NavigableSet<LogLevel> getLevels() {
			return this.levels;
		}

		/**
		 * Returns a map of loggers and their corresponding levels.
		 * @return a map containing loggers and their levels
		 */
		public Map<String, LoggerLevelsDescriptor> getLoggers() {
			return this.loggers;
		}

		/**
		 * Returns the map of group names to their corresponding
		 * GroupLoggerLevelsDescriptor objects.
		 * @return the map of group names to GroupLoggerLevelsDescriptor objects
		 */
		public Map<String, GroupLoggerLevelsDescriptor> getGroups() {
			return this.groups;
		}

	}

	/**
	 * Description of levels configured for a given logger.
	 */
	public static class LoggerLevelsDescriptor implements OperationResponseBody {

		private final String configuredLevel;

		/**
		 * Constructs a new LoggerLevelsDescriptor object with the specified configured
		 * level.
		 * @param configuredLevel the configured log level for the logger
		 */
		public LoggerLevelsDescriptor(LogLevel configuredLevel) {
			this.configuredLevel = (configuredLevel != null) ? configuredLevel.name() : null;
		}

		/**
		 * Constructs a new LoggerLevelsDescriptor with the specified direct
		 * configuration.
		 * @param directConfiguration the direct configuration for the logger levels (can
		 * be null)
		 */
		LoggerLevelsDescriptor(LevelConfiguration directConfiguration) {
			this.configuredLevel = (directConfiguration != null) ? directConfiguration.getName() : null;
		}

		/**
		 * Returns the name of the given log level.
		 * @param level the log level to get the name of
		 * @return the name of the log level, or null if the level is null
		 */
		protected final String getName(LogLevel level) {
			return (level != null) ? level.name() : null;
		}

		/**
		 * Returns the configured logging level.
		 * @return the configured logging level
		 */
		public String getConfiguredLevel() {
			return this.configuredLevel;
		}

	}

	/**
	 * Description of levels configured for a given group logger.
	 */
	public static class GroupLoggerLevelsDescriptor extends LoggerLevelsDescriptor {

		private final List<String> members;

		/**
		 * Constructs a new GroupLoggerLevelsDescriptor with the specified configured
		 * level and list of members.
		 * @param configuredLevel the configured log level for the group
		 * @param members the list of members in the group
		 */
		public GroupLoggerLevelsDescriptor(LogLevel configuredLevel, List<String> members) {
			super(configuredLevel);
			this.members = members;
		}

		/**
		 * Returns the list of members in the group.
		 * @return the list of members in the group
		 */
		public List<String> getMembers() {
			return this.members;
		}

	}

	/**
	 * Description of levels configured for a given single logger.
	 */
	public static class SingleLoggerLevelsDescriptor extends LoggerLevelsDescriptor {

		private final String effectiveLevel;

		/**
		 * Constructs a new SingleLoggerLevelsDescriptor object with the specified
		 * LoggerConfiguration.
		 * @param configuration the LoggerConfiguration to use for constructing the
		 * SingleLoggerLevelsDescriptor
		 */
		public SingleLoggerLevelsDescriptor(LoggerConfiguration configuration) {
			super(configuration.getLevelConfiguration(ConfigurationScope.DIRECT));
			this.effectiveLevel = configuration.getLevelConfiguration().getName();
		}

		/**
		 * Returns the effective logging level.
		 * @return the effective logging level
		 */
		public String getEffectiveLevel() {
			return this.effectiveLevel;
		}

	}

}
