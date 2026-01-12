/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.context.properties;

import java.util.Map;

/**
 * A configuration property.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConfigurationProperty {

	private final String name;

	private final String type;

	private final Object defaultValue;

	private final String description;

	private final boolean deprecated;

	private final Deprecation deprecation;

	ConfigurationProperty(String name, String type) {
		this(name, type, null, null, false, null);
	}

	ConfigurationProperty(String name, String type, Object defaultValue, String description, boolean deprecated,
			Deprecation deprecation) {
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
		this.description = description;
		this.deprecated = deprecated;
		this.deprecation = deprecation;
	}

	String getName() {
		return this.name;
	}

	String getDisplayName() {
		return (getType() != null && getType().startsWith("java.util.Map")) ? getName() + ".*" : getName();
	}

	String getType() {
		return this.type;
	}

	Object getDefaultValue() {
		return this.defaultValue;
	}

	String getDescription() {
		return this.description;
	}

	boolean isDeprecated() {
		return this.deprecated;
	}

	Deprecation getDeprecation() {
		return this.deprecation;
	}

	@Override
	public String toString() {
		return "ConfigurationProperty [name=" + this.name + ", type=" + this.type + "]";
	}

	@SuppressWarnings("unchecked")
	static ConfigurationProperty fromJsonProperties(Map<String, Object> property) {
		String name = (String) property.get("name");
		String type = (String) property.get("type");
		Object defaultValue = property.get("defaultValue");
		String description = (String) property.get("description");
		boolean deprecated = property.containsKey("deprecated");
		Map<String, Object> deprecation = (Map<String, Object>) property.get("deprecation");
		return new ConfigurationProperty(name, type, defaultValue, description, deprecated,
				Deprecation.fromJsonProperties(deprecation));
	}

	record Deprecation(String reason, String replacement, String since, String level) {

		static Deprecation fromJsonProperties(Map<String, Object> property) {
			if (property == null) {
				return null;
			}
			String reason = (String) property.get("reason");
			String replacement = (String) property.get("replacement");
			String since = (String) property.get("since");
			String level = (String) property.get("level");
			return new Deprecation(reason, replacement, since, level);
		}

	}

}
