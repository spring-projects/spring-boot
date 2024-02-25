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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A single logger group.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.2.0
 */
public final class LoggerGroup {

	private final String name;

	private final List<String> members;

	private LogLevel configuredLevel;

	/**
	 * Constructs a new LoggerGroup with the specified name and members.
	 * @param name the name of the LoggerGroup
	 * @param members the list of members in the LoggerGroup
	 */
	LoggerGroup(String name, List<String> members) {
		this.name = name;
		this.members = Collections.unmodifiableList(new ArrayList<>(members));
	}

	/**
	 * Returns the name of the LoggerGroup.
	 * @return the name of the LoggerGroup
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the list of members in the LoggerGroup.
	 * @return the list of members in the LoggerGroup
	 */
	public List<String> getMembers() {
		return this.members;
	}

	/**
	 * Checks if the LoggerGroup has any members.
	 * @return true if the LoggerGroup has members, false otherwise.
	 */
	public boolean hasMembers() {
		return !this.members.isEmpty();
	}

	/**
	 * Returns the configured log level.
	 * @return the configured log level
	 */
	public LogLevel getConfiguredLevel() {
		return this.configuredLevel;
	}

	/**
	 * Configures the log level for the LoggerGroup.
	 * @param level the log level to be configured
	 * @param configurer the BiConsumer used to configure the log level for each member of
	 * the LoggerGroup
	 */
	public void configureLogLevel(LogLevel level, BiConsumer<String, LogLevel> configurer) {
		this.configuredLevel = level;
		this.members.forEach((name) -> configurer.accept(name, level));
	}

}
