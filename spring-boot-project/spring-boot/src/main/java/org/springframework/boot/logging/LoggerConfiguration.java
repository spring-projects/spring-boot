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

package org.springframework.boot.logging;

import java.util.Objects;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Immutable class that represents the configuration of a {@link LoggingSystem}'s logger.
 *
 * @author Ben Hale
 * @author Phillip Webb
 * @since 1.5.0
 */
public final class LoggerConfiguration {

	private final String name;

	private final LevelConfiguration levelConfiguration;

	private final LevelConfiguration inheritedLevelConfiguration;

	/**
	 * Create a new {@link LoggerConfiguration instance}.
	 * @param name the name of the logger
	 * @param configuredLevel the configured level of the logger
	 * @param effectiveLevel the effective level of the logger
	 */
	public LoggerConfiguration(String name, LogLevel configuredLevel, LogLevel effectiveLevel) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(effectiveLevel, "EffectiveLevel must not be null");
		this.name = name;
		this.levelConfiguration = (configuredLevel != null) ? LevelConfiguration.of(configuredLevel) : null;
		this.inheritedLevelConfiguration = LevelConfiguration.of(effectiveLevel);
	}

	/**
	 * Create a new {@link LoggerConfiguration instance}.
	 * @param name the name of the logger
	 * @param levelConfiguration the level configuration
	 * @param inheritedLevelConfiguration the inherited level configuration
	 * @since 2.7.13
	 */
	public LoggerConfiguration(String name, LevelConfiguration levelConfiguration,
			LevelConfiguration inheritedLevelConfiguration) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(inheritedLevelConfiguration, "InheritedLevelConfiguration must not be null");
		this.name = name;
		this.levelConfiguration = levelConfiguration;
		this.inheritedLevelConfiguration = inheritedLevelConfiguration;
	}

	/**
	 * Returns the name of the logger.
	 * @return the name of the logger
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the configured level of the logger.
	 * @return the configured level of the logger
	 * @see #getLevelConfiguration(ConfigurationScope)
	 */
	public LogLevel getConfiguredLevel() {
		LevelConfiguration configuration = getLevelConfiguration(ConfigurationScope.DIRECT);
		return (configuration != null) ? configuration.getLevel() : null;
	}

	/**
	 * Returns the effective level of the logger.
	 * @return the effective level of the logger
	 * @see #getLevelConfiguration(ConfigurationScope)
	 */
	public LogLevel getEffectiveLevel() {
		return getLevelConfiguration().getLevel();
	}

	/**
	 * Return the level configuration, considering inherited loggers.
	 * @return the level configuration
	 * @since 2.7.13
	 */
	public LevelConfiguration getLevelConfiguration() {
		return getLevelConfiguration(ConfigurationScope.INHERITED);
	}

	/**
	 * Return the level configuration for the given scope.
	 * @param scope the configuration scope
	 * @return the level configuration or {@code null} for
	 * {@link ConfigurationScope#DIRECT direct scope} results without applied
	 * configuration
	 * @since 2.7.13
	 */
	public LevelConfiguration getLevelConfiguration(ConfigurationScope scope) {
		return (scope != ConfigurationScope.DIRECT) ? this.inheritedLevelConfiguration : this.levelConfiguration;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		LoggerConfiguration other = (LoggerConfiguration) obj;
		return ObjectUtils.nullSafeEquals(this.name, other.name)
				&& ObjectUtils.nullSafeEquals(this.levelConfiguration, other.levelConfiguration)
				&& ObjectUtils.nullSafeEquals(this.inheritedLevelConfiguration, other.inheritedLevelConfiguration);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.levelConfiguration, this.inheritedLevelConfiguration);
	}

	@Override
	public String toString() {
		return "LoggerConfiguration [name=" + this.name + ", levelConfiguration=" + this.levelConfiguration
				+ ", inheritedLevelConfiguration=" + this.inheritedLevelConfiguration + "]";
	}

	/**
	 * Supported logger configuration scopes.
	 *
	 * @since 2.7.13
	 */
	public enum ConfigurationScope {

		/**
		 * Only return configuration that has been applied directly. Often referred to as
		 * 'configured' or 'assigned' configuration.
		 */
		DIRECT,

		/**
		 * May return configuration that has been applied to a parent logger. Often
		 * referred to as 'effective' configuration.
		 */
		INHERITED

	}

	/**
	 * Logger level configuration.
	 *
	 * @since 2.7.13
	 */
	public static final class LevelConfiguration {

		private final String name;

		private final LogLevel logLevel;

		private LevelConfiguration(String name, LogLevel logLevel) {
			this.name = name;
			this.logLevel = logLevel;
		}

		/**
		 * Return the name of the level.
		 * @return the level name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Return the actual level value if possible.
		 * @return the level value
		 * @throws IllegalStateException if this is a {@link #isCustom() custom} level
		 */
		public LogLevel getLevel() {
			Assert.state(this.logLevel != null, "Unable to provide LogLevel for '" + this.name + "'");
			return this.logLevel;
		}

		/**
		 * Return if this is a custom level and cannot be represented by {@link LogLevel}.
		 * @return if this is a custom level
		 */
		public boolean isCustom() {
			return this.logLevel == null;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			LevelConfiguration other = (LevelConfiguration) obj;
			return this.logLevel == other.logLevel && ObjectUtils.nullSafeEquals(this.name, other.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.logLevel, this.name);
		}

		@Override
		public String toString() {
			return "LevelConfiguration [name=" + this.name + ", logLevel=" + this.logLevel + "]";
		}

		/**
		 * Create a new {@link LevelConfiguration} instance of the given {@link LogLevel}.
		 * @param logLevel the log level
		 * @return a new {@link LevelConfiguration} instance
		 */
		public static LevelConfiguration of(LogLevel logLevel) {
			Assert.notNull(logLevel, "LogLevel must not be null");
			return new LevelConfiguration(logLevel.name(), logLevel);
		}

		/**
		 * Create a new {@link LevelConfiguration} instance for a custom level name.
		 * @param name the log level name
		 * @return a new {@link LevelConfiguration} instance
		 */
		public static LevelConfiguration ofCustom(String name) {
			Assert.hasText(name, "Name must not be empty");
			return new LevelConfiguration(name, null);
		}

	}

}
