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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * Manage logger groups.
 *
 * @author HaiTao Zhang
 * @since 2.2.0
 */
public class LoggingGroups {

	private Map<String, LogLevel> loggerGroupConfigurations;

	private Map<String, List<String>> loggerGroups;

	private LoggingSystem loggingSystem;

	public LoggingGroups(LoggingSystem loggingSystem) {
		this.loggerGroupConfigurations = new ConcurrentHashMap<>();
		this.loggerGroups = new ConcurrentHashMap<>();
		this.loggingSystem = loggingSystem;
	}

	/**
	 * Associate a name to a list of logger's name to create a logger group.
	 * @param groupName name of the logger group
	 * @param members list of the members names
	 */
	public void setLoggerGroup(String groupName, List<String> members) {
		Assert.notNull(groupName, "Group name can not be null");
		Assert.notNull(members, "Members can not be null");
		this.loggerGroups.put(groupName, members);
	}

	/**
	 * Set the logging level for a given logger group.
	 * @param groupName the name of the group to set
	 * @param level the log level ({@code null}) can be used to remove any custom level
	 * for the logger group and use the default configuration instead.
	 */
	public void setLoggerGroupLevel(String groupName, LogLevel level) {
		Assert.notNull(groupName, "Group name can not be null");
		List<String> members = this.loggerGroups.get(groupName);
		members.forEach((member) -> this.loggingSystem
				.setLogLevel(member.equalsIgnoreCase(LoggingSystem.ROOT_LOGGER_NAME) ? null : member, level));
		this.loggerGroupConfigurations.put(groupName, level);
	}

	/**
	 * Checks whether a groupName is associated to a logger group.
	 * @param groupName name of the logger group
	 * @return a boolean stating true when groupName is associated with a group of loggers
	 */
	public boolean isGroup(String groupName) {
		Assert.notNull(groupName, "Group name can not be null");
		return this.loggerGroups.containsKey(groupName);
	}

	/**
	 * Get the all registered logger groups.
	 * @return a Set of the names of the logger groups
	 */
	public Set<String> getLoggerGroupNames() {
		synchronized (this) {
			return this.loggerGroups.isEmpty() ? null : Collections.unmodifiableSet(this.loggerGroups.keySet());
		}
	}

	/**
	 * Get a logger group's members.
	 * @param groupName name of the logger group
	 * @return list of the members names associated with this group
	 */
	public List<String> getLoggerGroup(String groupName) {
		Assert.notNull(groupName, "Group name can not be null");
		return Collections.unmodifiableList(this.loggerGroups.get(groupName));
	}

	/**
	 * Get a logger group's configured level.
	 * @param groupName name of the logger group
	 * @return the logger groups configured level
	 */
	public LogLevel getLoggerGroupConfiguredLevel(String groupName) {
		Assert.notNull(groupName, "Group name can not be null");
		return this.loggerGroupConfigurations.get(groupName);
	}

}
