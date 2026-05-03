/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Used by {@link ConfigurationMetadataAnnotationProcessor} to collect
 * {@link ConfigurationMetadata}.
 *
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @author Moritz Halbritter
 * @author Stephane Nicoll
 */
class MetadataCollector {

	private final Predicate<ItemMetadata> mergeRequired;

	private final ConfigurationMetadata previousMetadata;

	private final Set<ItemMetadata> metadataItems = new LinkedHashSet<>();

	private final Set<ItemHint> metadataHints = new LinkedHashSet<>();

	/**
	 * Creates a new {@code MetadataProcessor} instance.
	 * @param mergeRequired specify whether an item can be merged
	 * @param previousMetadata any previous metadata or {@code null}
	 */
	MetadataCollector(Predicate<ItemMetadata> mergeRequired, ConfigurationMetadata previousMetadata) {
		this.mergeRequired = mergeRequired;
		this.previousMetadata = previousMetadata;
	}

	void add(ItemMetadata metadata) {
		this.metadataItems.add(metadata);
	}

	void add(ItemMetadata metadata, Consumer<ItemMetadata> onConflict) {
		ItemMetadata existing = find(metadata.getName());
		if (existing != null) {
			onConflict.accept(existing);
			return;
		}
		add(metadata);
	}

	boolean addIfAbsent(ItemMetadata metadata) {
		ItemMetadata existing = find(metadata.getName());
		if (existing != null) {
			return false;
		}
		add(metadata);
		return true;
	}

	void add(ItemHint itemHint) {
		this.metadataHints.add(itemHint);
	}

	boolean hasSimilarGroup(ItemMetadata metadata) {
		if (!metadata.isOfItemType(ItemMetadata.ItemType.GROUP)) {
			throw new IllegalStateException("item " + metadata + " must be a group");
		}
		for (ItemMetadata existing : this.metadataItems) {
			if (existing.isOfItemType(ItemMetadata.ItemType.GROUP) && existing.getName().equals(metadata.getName())
					&& existing.getType().equals(metadata.getType())) {
				return true;
			}
		}
		return false;
	}

	ConfigurationMetadata getMetadata() {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		for (ItemMetadata item : this.metadataItems) {
			metadata.add(item);
		}
		for (ItemHint metadataHint : this.metadataHints) {
			metadata.add(metadataHint);
		}
		if (this.previousMetadata != null) {
			List<ItemMetadata> items = this.previousMetadata.getItems();
			for (ItemMetadata item : items) {
				if (this.mergeRequired.test(item)) {
					metadata.addIfMissing(item);
				}
			}
		}
		return metadata;
	}

	private ItemMetadata find(String name) {
		return this.metadataItems.stream()
			.filter((candidate) -> name.equals(candidate.getName()))
			.findFirst()
			.orElse(null);
	}

}
