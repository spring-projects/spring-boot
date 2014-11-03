/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.configurationmetadata;

/**
 * Define a configuration item.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigurationMetadataProperty {

	private String id;

	private String name;

	private String type;

	private String description;

	private Object defaultValue;

	private boolean deprecated;

	/**
	 * The full identifier of the property, in lowercase dashed form (e.g. my.group.simple-property)
	 */
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * The name of the property, in lowercase dashed form (e.g. simple-property). If this item
	 * does not belong to any group, the id is returned.
	 */
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The class name of the data type of the property. For example, {@code java.lang.String}.
	 * <p>For consistency, the type of a primitive is specified using its wrapper counterpart,
	 * i.e. {@code boolean} becomes {@code java.lang.Boolean}. Collections type are harmonized
	 * to their interface counterpart and defines the actual generic types, i.e.
	 * {@code java.util.HashMap<java.lang.String,java.lang.Integer>} becomes
	 * {@code java.util.Map<java.lang.String,java.lang.Integer>}
	 * <p>Note that this class may be a complex type that gets converted from a String as values
	 * are bound.
	 */
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * A short description of the property, if any.
	 */
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * The default value, if any.
	 */
	public Object getDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Specify if the property is deprecated
	 */
	public boolean isDeprecated() {
		return this.deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

}
