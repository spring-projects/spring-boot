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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logger groups configured through the Spring Environment.
 *
 * @author HaiTao Zhang
 * @author Phillip Webb
 * @since 2.2.0
 * @see LoggerGroup
 */
public final class LoggerGroups implements Iterable<LoggerGroup> {

	private final Map<String, LoggerGroup> groups = new ConcurrentHashMap<>();

	/**
     * Constructs a new LoggerGroups object.
     */
    public LoggerGroups() {
	}

	/**
     * Constructs a new LoggerGroups object with the specified names and members.
     * 
     * @param namesAndMembers a Map containing the names of the logger groups as keys and a List of members as values
     */
    public LoggerGroups(Map<String, List<String>> namesAndMembers) {
		putAll(namesAndMembers);
	}

	/**
     * Puts all the key-value pairs from the given map into the logger groups.
     * 
     * @param namesAndMembers the map containing the names and members of the logger groups
     */
    public void putAll(Map<String, List<String>> namesAndMembers) {
		namesAndMembers.forEach(this::put);
	}

	/**
     * Adds a new LoggerGroup with the given name and members to the LoggerGroups collection.
     * 
     * @param name    the name of the LoggerGroup
     * @param members the list of members to be added to the LoggerGroup
     */
    private void put(String name, List<String> members) {
		put(new LoggerGroup(name, members));
	}

	/**
     * Adds a LoggerGroup to the collection of groups.
     * 
     * @param loggerGroup the LoggerGroup to be added
     */
    private void put(LoggerGroup loggerGroup) {
		this.groups.put(loggerGroup.getName(), loggerGroup);
	}

	/**
     * Retrieves the LoggerGroup object associated with the specified name.
     * 
     * @param name the name of the LoggerGroup to retrieve
     * @return the LoggerGroup object associated with the specified name, or null if no such LoggerGroup exists
     */
    public LoggerGroup get(String name) {
		return this.groups.get(name);
	}

	/**
     * Returns an iterator over the elements in this LoggerGroups object.
     *
     * @return an iterator over the elements in this LoggerGroups object
     */
    @Override
	public Iterator<LoggerGroup> iterator() {
		return this.groups.values().iterator();
	}

}
