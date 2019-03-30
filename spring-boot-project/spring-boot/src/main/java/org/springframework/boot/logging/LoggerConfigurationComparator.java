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

package org.springframework.boot.logging;

import java.util.Comparator;

import org.springframework.util.Assert;

/**
 * An implementation of {@link Comparator} for comparing {@link LoggerConfiguration}s.
 * Sorts the "root" logger as the first logger and then lexically by name after that.
 *
 * @author Ben Hale
 */
class LoggerConfigurationComparator implements Comparator<LoggerConfiguration> {

	private final String rootLoggerName;

	/**
	 * Create a new {@link LoggerConfigurationComparator} instance.
	 * @param rootLoggerName the name of the "root" logger
	 */
	LoggerConfigurationComparator(String rootLoggerName) {
		Assert.notNull(rootLoggerName, "RootLoggerName must not be null");
		this.rootLoggerName = rootLoggerName;
	}

	@Override
	public int compare(LoggerConfiguration o1, LoggerConfiguration o2) {
		if (this.rootLoggerName.equals(o1.getName())) {
			return -1;
		}
		if (this.rootLoggerName.equals(o2.getName())) {
			return 1;
		}
		return o1.getName().compareTo(o2.getName());
	}

}
