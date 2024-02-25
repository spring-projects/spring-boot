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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Configuration meta-data.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 * @see ItemMetadata
 */
public class ConfigurationMetadata {

	private static final Set<Character> SEPARATORS;

	static {
		List<Character> chars = Arrays.asList('-', '_');
		SEPARATORS = Collections.unmodifiableSet(new HashSet<>(chars));
	}

	private final Map<String, List<ItemMetadata>> items;

	private final Map<String, List<ItemHint>> hints;

	/**
     * Constructs a new instance of ConfigurationMetadata.
     * Initializes the items and hints maps as empty LinkedHashMaps.
     */
    public ConfigurationMetadata() {
		this.items = new LinkedHashMap<>();
		this.hints = new LinkedHashMap<>();
	}

	/**
     * Creates a new instance of ConfigurationMetadata by copying the values from the given metadata object.
     * 
     * @param metadata the ConfigurationMetadata object to be copied
     */
    public ConfigurationMetadata(ConfigurationMetadata metadata) {
		this.items = new LinkedHashMap<>(metadata.items);
		this.hints = new LinkedHashMap<>(metadata.hints);
	}

	/**
	 * Add item meta-data.
	 * @param itemMetadata the meta-data to add
	 */
	public void add(ItemMetadata itemMetadata) {
		add(this.items, itemMetadata.getName(), itemMetadata, false);
	}

	/**
	 * Add item meta-data if it's not already present.
	 * @param itemMetadata the meta-data to add
	 * @since 2.4.0
	 */
	public void addIfMissing(ItemMetadata itemMetadata) {
		add(this.items, itemMetadata.getName(), itemMetadata, true);
	}

	/**
	 * Add item hint.
	 * @param itemHint the item hint to add
	 */
	public void add(ItemHint itemHint) {
		add(this.hints, itemHint.getName(), itemHint, false);
	}

	/**
	 * Merge the content from another {@link ConfigurationMetadata}.
	 * @param metadata the {@link ConfigurationMetadata} instance to merge
	 */
	public void merge(ConfigurationMetadata metadata) {
		for (ItemMetadata additionalItem : metadata.getItems()) {
			mergeItemMetadata(additionalItem);
		}
		for (ItemHint itemHint : metadata.getHints()) {
			add(itemHint);
		}
	}

	/**
	 * Return item meta-data.
	 * @return the items
	 */
	public List<ItemMetadata> getItems() {
		return flattenValues(this.items);
	}

	/**
	 * Return hint meta-data.
	 * @return the hints
	 */
	public List<ItemHint> getHints() {
		return flattenValues(this.hints);
	}

	/**
     * Merges the provided ItemMetadata with the existing metadata in the ConfigurationMetadata.
     * If a matching ItemMetadata is found, the properties of the provided metadata are merged into the matching metadata.
     * If no matching ItemMetadata is found, the provided metadata is added to the ConfigurationMetadata.
     * 
     * @param metadata The ItemMetadata to be merged.
     */
    protected void mergeItemMetadata(ItemMetadata metadata) {
		ItemMetadata matching = findMatchingItemMetadata(metadata);
		if (matching != null) {
			if (metadata.getDescription() != null) {
				matching.setDescription(metadata.getDescription());
			}
			if (metadata.getDefaultValue() != null) {
				matching.setDefaultValue(metadata.getDefaultValue());
			}
			ItemDeprecation deprecation = metadata.getDeprecation();
			ItemDeprecation matchingDeprecation = matching.getDeprecation();
			if (deprecation != null) {
				if (matchingDeprecation == null) {
					matching.setDeprecation(deprecation);
				}
				else {
					if (deprecation.getReason() != null) {
						matchingDeprecation.setReason(deprecation.getReason());
					}
					if (deprecation.getReplacement() != null) {
						matchingDeprecation.setReplacement(deprecation.getReplacement());
					}
					if (deprecation.getLevel() != null) {
						matchingDeprecation.setLevel(deprecation.getLevel());
					}
					if (deprecation.getSince() != null) {
						matchingDeprecation.setSince(deprecation.getSince());
					}
				}
			}
		}
		else {
			add(this.items, metadata.getName(), metadata, false);
		}
	}

	/**
     * Adds a value to the specified key in the given map.
     *
     * @param <K>        the type of the key in the map
     * @param <V>        the type of the value in the map
     * @param map        the map to add the value to
     * @param key        the key to add the value to
     * @param value      the value to add
     * @param ifMissing  a flag indicating whether to add the value only if the key is missing or if the key already exists
     */
    private <K, V> void add(Map<K, List<V>> map, K key, V value, boolean ifMissing) {
		List<V> values = map.computeIfAbsent(key, (k) -> new ArrayList<>());
		if (!ifMissing || values.isEmpty()) {
			values.add(value);
		}
	}

	/**
     * Finds the matching ItemMetadata for the given metadata.
     * 
     * @param metadata The ItemMetadata to find a match for.
     * @return The matching ItemMetadata, or null if no match is found.
     */
    private ItemMetadata findMatchingItemMetadata(ItemMetadata metadata) {
		List<ItemMetadata> candidates = this.items.get(metadata.getName());
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		candidates = new ArrayList<>(candidates);
		candidates.removeIf((itemMetadata) -> !itemMetadata.hasSameType(metadata));
		if (candidates.size() > 1 && metadata.getType() != null) {
			candidates.removeIf((itemMetadata) -> !metadata.getType().equals(itemMetadata.getType()));
		}
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		for (ItemMetadata candidate : candidates) {
			if (nullSafeEquals(candidate.getSourceType(), metadata.getSourceType())) {
				return candidate;
			}
		}
		return null;
	}

	/**
     * Compares two objects for equality, taking into account null values.
     * 
     * @param o1 the first object to compare
     * @param o2 the second object to compare
     * @return true if the objects are equal, false otherwise
     */
    private boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		return o1 != null && o1.equals(o2);
	}

	/**
     * Generates a nested prefix by combining the given prefix and name.
     * If the prefix is null, an empty string is used as the prefix.
     * The name is converted to dashed case before combining with the prefix.
     * If the prefix is empty, the name is used as is.
     * The nested prefix is returned as a string.
     *
     * @param prefix the prefix to be combined with the name (can be null)
     * @param name   the name to be combined with the prefix
     * @return the nested prefix as a string
     */
    public static String nestedPrefix(String prefix, String name) {
		String nestedPrefix = (prefix != null) ? prefix : "";
		String dashedName = toDashedCase(name);
		nestedPrefix += nestedPrefix.isEmpty() ? dashedName : "." + dashedName;
		return nestedPrefix;
	}

	/**
     * Converts a given name to dashed case.
     * Dashed case is a naming convention where words are separated by dashes.
     * 
     * @param name the name to be converted
     * @return the name converted to dashed case
     */
    static String toDashedCase(String name) {
		StringBuilder dashed = new StringBuilder();
		Character previous = null;
		for (int i = 0; i < name.length(); i++) {
			char current = name.charAt(i);
			if (SEPARATORS.contains(current)) {
				dashed.append("-");
			}
			else if (Character.isUpperCase(current) && previous != null && !SEPARATORS.contains(previous)) {
				dashed.append("-").append(current);
			}
			else {
				dashed.append(current);
			}
			previous = current;

		}
		return dashed.toString().toLowerCase(Locale.ENGLISH);
	}

	/**
     * Flattens the values of a map into a single list and sorts them in ascending order.
     * 
     * @param map the map containing lists of values
     * @return a sorted list containing all the values from the map
     * @throws NullPointerException if the map is null
     * @param <T> the type of values in the map, must implement Comparable interface
     */
    private static <T extends Comparable<T>> List<T> flattenValues(Map<?, List<T>> map) {
		List<T> content = new ArrayList<>();
		for (List<T> values : map.values()) {
			content.addAll(values);
		}
		Collections.sort(content);
		return content;
	}

	/**
     * Returns a string representation of the ConfigurationMetadata object.
     * 
     * @return a string representation of the ConfigurationMetadata object
     */
    @Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(String.format("items: %n"));
		this.items.values().forEach((itemMetadata) -> result.append("\t").append(String.format("%s%n", itemMetadata)));
		return result.toString();
	}

}
