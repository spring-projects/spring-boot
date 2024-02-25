/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.cli.command.init;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represent a project type that is supported by a service.
 *
 * @author Stephane Nicoll
 */
class ProjectType {

	private final String id;

	private final String name;

	private final String action;

	private final boolean defaultType;

	private final Map<String, String> tags = new HashMap<>();

	/**
	 * Constructs a new ProjectType with the specified id, name, action, defaultType, and
	 * tags.
	 * @param id the id of the ProjectType
	 * @param name the name of the ProjectType
	 * @param action the action of the ProjectType
	 * @param defaultType the default type of the ProjectType
	 * @param tags the tags associated with the ProjectType
	 */
	ProjectType(String id, String name, String action, boolean defaultType, Map<String, String> tags) {
		this.id = id;
		this.name = name;
		this.action = action;
		this.defaultType = defaultType;
		if (tags != null) {
			this.tags.putAll(tags);
		}
	}

	/**
	 * Returns the ID of the ProjectType.
	 * @return the ID of the ProjectType
	 */
	String getId() {
		return this.id;
	}

	/**
	 * Returns the name of the ProjectType.
	 * @return the name of the ProjectType
	 */
	String getName() {
		return this.name;
	}

	/**
	 * Returns the action of the ProjectType.
	 * @return the action of the ProjectType
	 */
	String getAction() {
		return this.action;
	}

	/**
	 * Returns a boolean value indicating whether this ProjectType is the default type.
	 * @return true if this ProjectType is the default type, false otherwise.
	 */
	boolean isDefaultType() {
		return this.defaultType;
	}

	/**
	 * Returns an unmodifiable map of tags.
	 * @return an unmodifiable map of tags
	 */
	Map<String, String> getTags() {
		return Collections.unmodifiableMap(this.tags);
	}

}
