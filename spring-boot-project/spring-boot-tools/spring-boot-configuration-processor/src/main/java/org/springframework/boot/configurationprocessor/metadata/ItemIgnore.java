/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Objects;

import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * Ignored item.
 *
 * @author Moritz Halbritter
 * @since 3.5.0
 */
public final class ItemIgnore implements Comparable<ItemIgnore> {

	private final ItemType type;

	private final String name;

	private ItemIgnore(ItemType type, String name) {
		if (type == null) {
			throw new IllegalArgumentException("'type' must not be null");
		}
		if (name == null) {
			throw new IllegalArgumentException("'name' must not be null");
		}
		this.type = type;
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public ItemType getType() {
		return this.type;
	}

	@Override
	public int compareTo(ItemIgnore other) {
		return getName().compareTo(other.getName());
	}

	/**
	 * Create an ignore for a property with the given name.
	 * @param name the name
	 * @return the item ignore
	 */
	public static ItemIgnore forProperty(String name) {
		return new ItemIgnore(ItemType.PROPERTY, name);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ItemIgnore that = (ItemIgnore) o;
		return this.type == that.type && Objects.equals(this.name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.name);
	}

	@Override
	public String toString() {
		return "ItemIgnore{" + "type=" + this.type + ", name='" + this.name + '\'' + '}';
	}

}
