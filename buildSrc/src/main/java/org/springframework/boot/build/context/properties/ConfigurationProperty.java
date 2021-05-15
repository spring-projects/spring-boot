/*
 * Copyright 2019-2020 the original author or authors.
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

/**
 * A configuration property.
 *
 * @author Andy Wilkinson
 */
public class ConfigurationProperty {

	private final String name;

	private final String type;

	private final Object defaultValue;

	private final String description;

	private final boolean deprecated;

	ConfigurationProperty(String name, String type) {
		this(name, type, null, null, false);
	}

	ConfigurationProperty(String name, String type, Object defaultValue, String description, boolean deprecated) {
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
		this.description = description;
		this.deprecated = deprecated;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	public Object getDefaultValue() {
		return this.defaultValue;
	}

	public String getDescription() {
		return this.description;
	}

	public boolean isDeprecated() {
		return this.deprecated;
	}

	@Override
	public String toString() {
		return "ConfigurationProperty [name=" + this.name + ", type=" + this.type + "]";
	}

}
