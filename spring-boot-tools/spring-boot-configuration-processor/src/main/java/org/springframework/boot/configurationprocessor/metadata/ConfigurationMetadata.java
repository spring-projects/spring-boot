/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

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
		SEPARATORS = Collections.unmodifiableSet(new HashSet<Character>(chars));
	}

	private final MultiValueMap<String, ItemMetadata> items;

	private final MultiValueMap<String, ItemHint> hints;

	public ConfigurationMetadata() {
		this.items = new LinkedMultiValueMap<String, ItemMetadata>();
		this.hints = new LinkedMultiValueMap<String, ItemHint>();
	}

	public ConfigurationMetadata(ConfigurationMetadata metadata) {
		this.items = new LinkedMultiValueMap<String, ItemMetadata>(metadata.items);
		this.hints = new LinkedMultiValueMap<String, ItemHint>(metadata.hints);
	}

	/**
	 * Add item meta-data.
	 * @param itemMetadata the meta-data to add
	 */
	public void add(ItemMetadata itemMetadata) {
		this.items.add(itemMetadata.getName(), itemMetadata);
	}

	/**
	 * Add item hint.
	 * @param itemHint the item hint to add
	 */
	public void add(ItemHint itemHint) {
		this.hints.add(itemHint.getName(), itemHint);
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
				}
			}
		}
		else {
			this.items.add(metadata.getName(), metadata);
		}
	}

	private ItemMetadata findMatchingItemMetadata(ItemMetadata metadata) {
		List<ItemMetadata> candidates = this.items.get(metadata.getName());
		if (CollectionUtils.isEmpty(candidates)) {
			return null;
		}
		ListIterator<ItemMetadata> it = candidates.listIterator();
		while (it.hasNext()) {
			if (!it.next().hasSameType(metadata)) {
				it.remove();
			}
		}
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		for (ItemMetadata candidate : candidates) {
			if (ObjectUtils.nullSafeEquals(candidate.getSourceType(),
					metadata.getSourceType())) {
				return candidate;
			}
		}
		return null;
	}

	public static String nestedPrefix(String prefix, String name) {
		String nestedPrefix = (prefix == null ? "" : prefix);
		String dashedName = toDashedCase(name);
		nestedPrefix += ("".equals(nestedPrefix) ? dashedName : "." + dashedName);
		return nestedPrefix;
	}

	static String toDashedCase(String name) {
		StringBuilder dashed = new StringBuilder();
		Character previous = null;
		for (char current : name.toCharArray()) {
			if (SEPARATORS.contains(current)) {
				dashed.append("-");
			}
			else if (Character.isUpperCase(current) && previous != null
					&& !SEPARATORS.contains(previous)) {
				dashed.append("-").append(current);
			}
			else {
				dashed.append(current);
			}
			previous = current;

		}
		return dashed.toString().toLowerCase();
	}

	private static <T extends Comparable<T>> List<T> flattenValues(
			MultiValueMap<?, T> map) {
		List<T> content = new ArrayList<T>();
		for (List<T> values : map.values()) {
			content.addAll(values);
		}
		Collections.sort(content);
		return content;
	}

}
