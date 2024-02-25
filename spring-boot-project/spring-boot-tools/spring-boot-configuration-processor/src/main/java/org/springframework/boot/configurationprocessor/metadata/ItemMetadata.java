/*
 * Copyright 2012-2024 the original author or authors.
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

	private final ItemType itemType;

	private String name;

	private String type;

	private String description;

	private String sourceType;

	private String sourceMethod;

	private Object defaultValue;

	private ItemDeprecation deprecation;

	/**
	 * Constructs a new ItemMetadata object with the specified parameters.
	 * @param itemType the type of the item
	 * @param prefix the prefix of the item name
	 * @param name the name of the item
	 * @param type the type of the item
	 * @param sourceType the source type of the item
	 * @param sourceMethod the source method of the item
	 * @param description the description of the item
	 * @param defaultValue the default value of the item
	 * @param deprecation the deprecation information of the item
	 */
	ItemMetadata(ItemType itemType, String prefix, String name, String type, String sourceType, String sourceMethod,
			String description, Object defaultValue, ItemDeprecation deprecation) {
		this.itemType = itemType;
		this.name = buildName(prefix, name);
		this.type = type;
		this.sourceType = sourceType;
		this.sourceMethod = sourceMethod;
		this.description = description;
		this.defaultValue = defaultValue;
		this.deprecation = deprecation;
	}

	/**
	 * Builds a full name by concatenating a prefix and a name.
	 * @param prefix the prefix to be added to the full name (optional)
	 * @param name the name to be added to the full name (optional)
	 * @return the full name built by concatenating the prefix and name
	 */
	private String buildName(String prefix, String name) {
		StringBuilder fullName = new StringBuilder();
		if (prefix != null) {
			if (prefix.endsWith(".")) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}
			fullName.append(prefix);
		}
		if (name != null) {
			if (!fullName.isEmpty()) {
				fullName.append('.');
			}
			fullName.append(ConfigurationMetadata.toDashedCase(name));
		}
		return fullName.toString();
	}

	/**
	 * Checks if the item is of the specified item type.
	 * @param itemType the item type to check against
	 * @return true if the item is of the specified item type, false otherwise
	 */
	public boolean isOfItemType(ItemType itemType) {
		return this.itemType == itemType;
	}

	/**
	 * Checks if the item has the same type as the given metadata.
	 * @param metadata the metadata to compare with
	 * @return true if the item has the same type as the metadata, false otherwise
	 */
	public boolean hasSameType(ItemMetadata metadata) {
		return this.itemType == metadata.itemType;
	}

	/**
	 * Returns the name of the item.
	 * @return the name of the item
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the item.
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the type of the item.
	 * @return the type of the item
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Sets the type of the item.
	 * @param type the type of the item
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the description of the item.
	 * @return the description of the item
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the description of the item.
	 * @param description the description to be set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Returns the source type of the item.
	 * @return the source type of the item
	 */
	public String getSourceType() {
		return this.sourceType;
	}

	/**
	 * Sets the source type of the item metadata.
	 * @param sourceType the source type to be set
	 */
	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	/**
	 * Returns the source method of the ItemMetadata object.
	 * @return the source method of the ItemMetadata object
	 */
	public String getSourceMethod() {
		return this.sourceMethod;
	}

	/**
	 * Sets the source method for the ItemMetadata.
	 * @param sourceMethod the source method to be set
	 */
	public void setSourceMethod(String sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

	/**
	 * Returns the default value of the item metadata.
	 * @return the default value of the item metadata
	 */
	public Object getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * Sets the default value for the item metadata.
	 * @param defaultValue the default value to be set
	 */
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Returns the deprecation information of the item.
	 * @return the deprecation information of the item
	 */
	public ItemDeprecation getDeprecation() {
		return this.deprecation;
	}

	/**
	 * Sets the deprecation information for this item.
	 * @param deprecation the deprecation information to be set
	 */
	public void setDeprecation(ItemDeprecation deprecation) {
		this.deprecation = deprecation;
	}

	/**
	 * Compares this ItemMetadata object to the specified object for equality. Returns
	 * true if the specified object is also an ItemMetadata object and all the
	 * corresponding fields have the same values, otherwise returns false.
	 * @param o the object to compare this ItemMetadata object against
	 * @return true if the given object is equal to this ItemMetadata object, false
	 * otherwise
	 */
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

	/**
	 * Returns the hash code value for this ItemMetadata object.
	 *
	 * The hash code is calculated based on the values of the following fields: - itemType
	 * - name - type - description - sourceType - sourceMethod - defaultValue -
	 * deprecation
	 * @return the hash code value for this ItemMetadata object
	 */
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

	/**
	 * Compares two objects for equality, taking into account null values.
	 * @param o1 the first object to compare
	 * @param o2 the second object to compare
	 * @return true if the objects are equal, false otherwise
	 */
	private boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	/**
	 * Calculates the hash code of the given object, handling null values.
	 * @param o the object to calculate the hash code for
	 * @return the hash code of the object, or 0 if the object is null
	 */
	private int nullSafeHashCode(Object o) {
		return (o != null) ? o.hashCode() : 0;
	}

	/**
	 * Returns a string representation of the ItemMetadata object.
	 * @return a string representation of the ItemMetadata object
	 */
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

	/**
	 * Builds a string representation of a property and its value and appends it to the
	 * given StringBuilder.
	 * @param string the StringBuilder to append the string representation to
	 * @param property the name of the property
	 * @param value the value of the property
	 */
	private void buildToStringProperty(StringBuilder string, String property, Object value) {
		if (value != null) {
			string.append(" ").append(property).append(":").append(value);
		}
	}

	/**
	 * Compares this ItemMetadata object with the specified ItemMetadata object for order
	 * based on their names.
	 * @param o the ItemMetadata object to be compared
	 * @return a negative integer, zero, or a positive integer as this ItemMetadata object
	 * is less than, equal to, or greater than the specified ItemMetadata object
	 * @throws NullPointerException if the specified ItemMetadata object is null
	 */
	@Override
	public int compareTo(ItemMetadata o) {
		return getName().compareTo(o.getName());
	}

	/**
	 * Creates a new group item metadata with the specified name, type, source type, and
	 * source method.
	 * @param name the name of the group
	 * @param type the type of the group
	 * @param sourceType the source type of the group
	 * @param sourceMethod the source method of the group
	 * @return the newly created group item metadata
	 */
	public static ItemMetadata newGroup(String name, String type, String sourceType, String sourceMethod) {
		return new ItemMetadata(ItemType.GROUP, name, null, type, sourceType, sourceMethod, null, null, null);
	}

	/**
	 * Creates a new property with the specified details.
	 * @param prefix the prefix of the property
	 * @param name the name of the property
	 * @param type the type of the property
	 * @param sourceType the source type of the property
	 * @param sourceMethod the source method of the property
	 * @param description the description of the property
	 * @param defaultValue the default value of the property
	 * @param deprecation the deprecation details of the property
	 * @return the newly created ItemMetadata object representing the property
	 */
	public static ItemMetadata newProperty(String prefix, String name, String type, String sourceType,
			String sourceMethod, String description, Object defaultValue, ItemDeprecation deprecation) {
		return new ItemMetadata(ItemType.PROPERTY, prefix, name, type, sourceType, sourceMethod, description,
				defaultValue, deprecation);
	}

	/**
	 * Generates a new item metadata prefix by concatenating the given prefix and suffix.
	 * The prefix is converted to lowercase using the English locale. The suffix is
	 * converted to dashed case.
	 * @param prefix the prefix to be used
	 * @param suffix the suffix to be used
	 * @return the new item metadata prefix
	 */
	public static String newItemMetadataPrefix(String prefix, String suffix) {
		return prefix.toLowerCase(Locale.ENGLISH) + ConfigurationMetadata.toDashedCase(suffix);
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
