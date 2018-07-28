/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.Locale;

/**
 * A group or property meta-data item from some {@link ConfigurationMetadata}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 * @see ConfigurationMetadata
 */
public final class ItemMetadata implements Comparable<ItemMetadata> {

	private ItemType itemType;

	private String name;

	private String type;

	private String description;

	private String sourceType;

	private String sourceMethod;

	private Object defaultValue;

	private ItemDeprecation deprecation;

	ItemMetadata(ItemType itemType, String prefix, String name, String type,
			String sourceType, String sourceMethod, String description,
			Object defaultValue, ItemDeprecation deprecation) {
		this.itemType = itemType;
		this.name = buildName(prefix, name);
		this.type = type;
		this.sourceType = sourceType;
		this.sourceMethod = sourceMethod;
		this.description = description;
		this.defaultValue = defaultValue;
		this.deprecation = deprecation;
	}

	private String buildName(String prefix, String name) {
		while (prefix != null && prefix.endsWith(".")) {
			prefix = prefix.substring(0, prefix.length() - 1);
		}
		StringBuilder fullName = new StringBuilder((prefix != null) ? prefix : "");
		if (fullName.length() > 0 && name != null) {
			fullName.append(".");
		}
		fullName.append((name != null) ? ConfigurationMetadata.toDashedCase(name) : "");
		return fullName.toString();
	}

	public boolean isOfItemType(ItemType itemType) {
		return this.itemType == itemType;
	}

	public boolean hasSameType(ItemMetadata metadata) {
		return this.itemType == metadata.itemType;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSourceType() {
		return this.sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getSourceMethod() {
		return this.sourceMethod;
	}

	public void setSourceMethod(String sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

	public Object getDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public ItemDeprecation getDeprecation() {
		return this.deprecation;
	}

	public void setDeprecation(ItemDeprecation deprecation) {
		this.deprecation = deprecation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ItemMetadata other = (ItemMetadata) o;
		boolean result = true;
		result = result && nullSafeEquals(this.itemType, other.itemType);
		result = result && nullSafeEquals(this.name, other.name);
		result = result && nullSafeEquals(this.type, other.type);
		result = result && nullSafeEquals(this.description, other.description);
		result = result && nullSafeEquals(this.sourceType, other.sourceType);
		result = result && nullSafeEquals(this.sourceMethod, other.sourceMethod);
		result = result && nullSafeEquals(this.defaultValue, other.defaultValue);
		result = result && nullSafeEquals(this.deprecation, other.deprecation);
		return result;
	}

	@Override
	public int hashCode() {
		int result = nullSafeHashCode(this.itemType);
		result = 31 * result + nullSafeHashCode(this.name);
		result = 31 * result + nullSafeHashCode(this.type);
		result = 31 * result + nullSafeHashCode(this.description);
		result = 31 * result + nullSafeHashCode(this.sourceType);
		result = 31 * result + nullSafeHashCode(this.sourceMethod);
		result = 31 * result + nullSafeHashCode(this.defaultValue);
		result = 31 * result + nullSafeHashCode(this.deprecation);
		return result;
	}

	private boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	private int nullSafeHashCode(Object o) {
		return (o != null) ? o.hashCode() : 0;
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder(this.name);
		buildToStringProperty(string, "type", this.type);
		buildToStringProperty(string, "sourceType", this.sourceType);
		buildToStringProperty(string, "description", this.description);
		buildToStringProperty(string, "defaultValue", this.defaultValue);
		buildToStringProperty(string, "deprecation", this.deprecation);
		return string.toString();
	}

	protected void buildToStringProperty(StringBuilder string, String property,
			Object value) {
		if (value != null) {
			string.append(" ").append(property).append(":").append(value);
		}
	}

	@Override
	public int compareTo(ItemMetadata o) {
		return getName().compareTo(o.getName());
	}

	public static ItemMetadata newGroup(String name, String type, String sourceType,
			String sourceMethod) {
		return new ItemMetadata(ItemType.GROUP, name, null, type, sourceType,
				sourceMethod, null, null, null);
	}

	public static ItemMetadata newProperty(String prefix, String name, String type,
			String sourceType, String sourceMethod, String description,
			Object defaultValue, ItemDeprecation deprecation) {
		return new ItemMetadata(ItemType.PROPERTY, prefix, name, type, sourceType,
				sourceMethod, description, defaultValue, deprecation);
	}

	public static String newItemMetadataPrefix(String prefix, String suffix) {
		return prefix.toLowerCase(Locale.ENGLISH)
				+ ConfigurationMetadata.toDashedCase(suffix);
	}

	/**
	 * The item type.
	 */
	public enum ItemType {

		/**
		 * Group item type.
		 */
		GROUP,

		/**
		 * Property item type.
		 */
		PROPERTY

	}

}
