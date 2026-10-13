/*
 * Copyright 2012-present the original author or authors.
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
