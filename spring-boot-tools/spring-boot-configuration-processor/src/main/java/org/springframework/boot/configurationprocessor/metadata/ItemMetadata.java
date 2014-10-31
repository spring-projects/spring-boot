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

package org.springframework.boot.configurationprocessor.metadata;

/**
 * A group or property meta-data item from some {@link ConfigurationMetadata}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 * @see ConfigurationMetadata
 */
public class ItemMetadata implements Comparable<ItemMetadata> {

	private final ItemType itemType;

	private final String name;

	private final String type;

	private final String description;

	private final String sourceType;

	private String sourceMethod;

	ItemMetadata(ItemType itemType, String prefix, String name, String type,
			String sourceType, String sourceMethod, String description) {
		super();
		this.itemType = itemType;
		this.name = buildName(prefix, name);
		this.type = type;
		this.sourceType = sourceType;
		this.sourceMethod = sourceMethod;
		this.description = description;
	}

	private String buildName(String prefix, String name) {
		while (prefix != null && prefix.endsWith(".")) {
			prefix = prefix.substring(0, prefix.length() - 1);
		}
		StringBuilder fullName = new StringBuilder(prefix == null ? "" : prefix);
		if (fullName.length() > 0 && name != null) {
			fullName.append(".");
		}
		fullName.append(name == null ? "" : ConfigurationMetadata.toDashedCase(name));
		return fullName.toString();
	}

	public boolean isOfItemType(ItemType itemType) {
		return this.itemType == itemType;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	public String getSourceType() {
		return this.sourceType;
	}

	public String getSourceMethod() {
		return this.sourceMethod;
	}

	public String getDescription() {
		return this.description;
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder(this.name);
		buildToStringProperty(string, "type", this.type);
		buildToStringProperty(string, "sourceType", this.sourceType);
		buildToStringProperty(string, "description", this.description);
		return string.toString();
	}

	protected final void buildToStringProperty(StringBuilder string, String property,
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
				sourceMethod, null);
	}

	public static ItemMetadata newProperty(String prefix, String name, String type,
			String sourceType, String sourceMethod, String description) {
		return new ItemMetadata(ItemType.PROPERTY, prefix, name, type, sourceType,
				sourceMethod, description);
	}

	/**
	 * The item type.
	 */
	public static enum ItemType {
		GROUP, PROPERTY
	}

}
