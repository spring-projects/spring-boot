/*
 * Copyright 2012-2021 the original author or authors.
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
 */
class ConfigurationProperty {

	private final String name;

	private final String type;

	private final Object defaultValue;

	private final String description;

	private final boolean deprecated;

	/**
	 * Constructs a new ConfigurationProperty with the specified name and type.
	 * @param name the name of the configuration property
	 * @param type the type of the configuration property
	 */
	ConfigurationProperty(String name, String type) {
		this(name, type, null, null, false);
	}

	/**
	 * Constructs a new ConfigurationProperty with the specified name, type, default
	 * value, description, and deprecation status.
	 * @param name the name of the configuration property
	 * @param type the type of the configuration property
	 * @param defaultValue the default value of the configuration property
	 * @param description the description of the configuration property
	 * @param deprecated true if the configuration property is deprecated, false otherwise
	 */
	ConfigurationProperty(String name, String type, Object defaultValue, String description, boolean deprecated) {
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
		this.description = description;
		this.deprecated = deprecated;
	}

	/**
	 * Returns the name of the ConfigurationProperty.
	 * @return the name of the ConfigurationProperty
	 */
	String getName() {
		return this.name;
	}

	/**
	 * Returns the display name of the configuration property. If the type of the property
	 * is not null and starts with "java.util.Map", the display name will be the name of
	 * the property followed by ".*". Otherwise, the display name will be the name of the
	 * property.
	 * @return the display name of the configuration property
	 */
	String getDisplayName() {
		return (getType() != null && getType().startsWith("java.util.Map")) ? getName() + ".*" : getName();
	}

	/**
	 * Returns the type of the configuration property.
	 * @return the type of the configuration property
	 */
	String getType() {
		return this.type;
	}

	/**
	 * Returns the default value of the configuration property.
	 * @return the default value of the configuration property
	 */
	Object getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * Returns the description of the configuration property.
	 * @return the description of the configuration property
	 */
	String getDescription() {
		return this.description;
	}

	/**
	 * Returns a boolean value indicating whether the configuration property is
	 * deprecated.
	 * @return {@code true} if the configuration property is deprecated, {@code false}
	 * otherwise.
	 */
	boolean isDeprecated() {
		return this.deprecated;
	}

	/**
	 * Returns a string representation of the ConfigurationProperty object.
	 * @return a string representation of the ConfigurationProperty object
	 */
	@Override
	public String toString() {
		return "ConfigurationProperty [name=" + this.name + ", type=" + this.type + "]";
	}

	/**
	 * Converts a map of properties in JSON format to a ConfigurationProperty object.
	 * @param property the map of properties in JSON format
	 * @return the ConfigurationProperty object created from the JSON properties
	 */
	static ConfigurationProperty fromJsonProperties(Map<String, Object> property) {
		String name = (String) property.get("name");
		String type = (String) property.get("type");
		Object defaultValue = property.get("defaultValue");
		String description = (String) property.get("description");
		boolean deprecated = property.containsKey("deprecated");
		return new ConfigurationProperty(name, type, defaultValue, description, deprecated);
	}

}
