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

package org.springframework.boot.logging;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Immutable class that represents the configuration of a {@link LoggingSystem}'s logger.
 *
 * @author Ben Hale
 * @since 1.5.0
 */
public final class LoggerConfiguration {

	private final String name;

	private final LogLevel configuredLevel;

	private final LogLevel effectiveLevel;

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
		this.configuredLevel = configuredLevel;
		this.effectiveLevel = effectiveLevel;
	}

	/**
	 * Returns the configured level of the logger.
	 * @return the configured level of the logger
	 */
	public LogLevel getConfiguredLevel() {
		return this.configuredLevel;
	}

	/**
	 * Returns the effective level of the logger.
	 * @return the effective level of the logger
	 */
	public LogLevel getEffectiveLevel() {
		return this.effectiveLevel;
	}

	/**
	 * Returns the name of the logger.
	 * @return the name of the logger
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof LoggerConfiguration) {
			LoggerConfiguration other = (LoggerConfiguration) obj;
			boolean rtn = true;
			rtn = rtn && ObjectUtils.nullSafeEquals(this.name, other.name);
			rtn = rtn && ObjectUtils.nullSafeEquals(this.configuredLevel, other.configuredLevel);
			rtn = rtn && ObjectUtils.nullSafeEquals(this.effectiveLevel, other.effectiveLevel);
			return rtn;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.configuredLevel);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.effectiveLevel);
		return result;
	}

	@Override
	public String toString() {
		return "LoggerConfiguration [name=" + this.name + ", configuredLevel=" + this.configuredLevel
				+ ", effectiveLevel=" + this.effectiveLevel + "]";
	}

}
