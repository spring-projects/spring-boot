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

package org.springframework.boot.configurationprocessor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Used by {@link ConfigurationMetadataAnnotationProcessor} to collect
 * {@link ConfigurationMetadata}.
 *
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @author Moritz Halbritter
 * @since 1.2.2
 */
public class MetadataCollector {

	private final Set<ItemMetadata> metadataItems = new LinkedHashSet<>();

	private final ProcessingEnvironment processingEnvironment;

	private final ConfigurationMetadata previousMetadata;

	private final TypeUtils typeUtils;

	private final Set<String> processedSourceTypes = new HashSet<>();

	/**
	 * Creates a new {@code MetadataProcessor} instance.
	 * @param processingEnvironment the processing environment of the build
	 * @param previousMetadata any previous metadata or {@code null}
	 */
	public MetadataCollector(ProcessingEnvironment processingEnvironment, ConfigurationMetadata previousMetadata) {
		this.processingEnvironment = processingEnvironment;
		this.previousMetadata = previousMetadata;
		this.typeUtils = new TypeUtils(processingEnvironment);
	}

	/**
	 * This method is responsible for processing the elements in the given
	 * RoundEnvironment. It iterates through each element in the round environment and
	 * marks it as processed.
	 * @param roundEnv the RoundEnvironment containing the elements to be processed
	 */
	public void processing(RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getRootElements()) {
			markAsProcessed(element);
		}
	}

	/**
	 * Marks the given element as processed.
	 * @param element the element to be marked as processed
	 */
	private void markAsProcessed(Element element) {
		if (element instanceof TypeElement) {
			this.processedSourceTypes.add(this.typeUtils.getQualifiedName(element));
		}
	}

	/**
	 * Adds an item metadata to the metadataItems list.
	 * @param metadata the item metadata to be added
	 */
	public void add(ItemMetadata metadata) {
		this.metadataItems.add(metadata);
	}

	/**
	 * Adds an item metadata to the collector.
	 * @param metadata the item metadata to be added
	 * @param onConflict the consumer to be executed when a conflict occurs with an
	 * existing item metadata
	 * @throws NullPointerException if the metadata is null
	 */
	public void add(ItemMetadata metadata, Consumer<ItemMetadata> onConflict) {
		ItemMetadata existing = find(metadata.getName());
		if (existing != null) {
			onConflict.accept(existing);
			return;
		}
		add(metadata);
	}

	/**
	 * Adds the given ItemMetadata to the MetadataCollector if it does not already exist.
	 * @param metadata the ItemMetadata to be added
	 * @return true if the ItemMetadata was added successfully, false if it already exists
	 */
	public boolean addIfAbsent(ItemMetadata metadata) {
		ItemMetadata existing = find(metadata.getName());
		if (existing != null) {
			return false;
		}
		add(metadata);
		return true;
	}

	/**
	 * Checks if the given ItemMetadata has a similar group in the MetadataCollector.
	 * @param metadata the ItemMetadata to check
	 * @return true if a similar group exists, false otherwise
	 * @throws IllegalStateException if the given metadata is not of type GROUP
	 */
	public boolean hasSimilarGroup(ItemMetadata metadata) {
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

	/**
	 * Retrieves the metadata for the configuration.
	 * @return The configuration metadata.
	 */
	public ConfigurationMetadata getMetadata() {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		for (ItemMetadata item : this.metadataItems) {
			metadata.add(item);
		}
		if (this.previousMetadata != null) {
			List<ItemMetadata> items = this.previousMetadata.getItems();
			for (ItemMetadata item : items) {
				if (shouldBeMerged(item)) {
					metadata.addIfMissing(item);
				}
			}
		}
		return metadata;
	}

	/**
	 * Finds an ItemMetadata object with the given name.
	 * @param name the name of the ItemMetadata object to find
	 * @return the found ItemMetadata object, or null if not found
	 */
	private ItemMetadata find(String name) {
		return this.metadataItems.stream()
			.filter((candidate) -> name.equals(candidate.getName()))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Determines whether the given item should be merged based on its metadata.
	 * @param itemMetadata the metadata of the item to be checked
	 * @return true if the item should be merged, false otherwise
	 */
	private boolean shouldBeMerged(ItemMetadata itemMetadata) {
		String sourceType = itemMetadata.getSourceType();
		return (sourceType != null && !deletedInCurrentBuild(sourceType) && !processedInCurrentBuild(sourceType));
	}

	/**
	 * Checks if the given source type has been deleted in the current build.
	 * @param sourceType the source type to check
	 * @return {@code true} if the source type has been deleted in the current build,
	 * {@code false} otherwise
	 */
	private boolean deletedInCurrentBuild(String sourceType) {
		return this.processingEnvironment.getElementUtils().getTypeElement(sourceType.replace('$', '.')) == null;
	}

	/**
	 * Checks if the given source type has been processed in the current build.
	 * @param sourceType the source type to check
	 * @return true if the source type has been processed in the current build, false
	 * otherwise
	 */
	private boolean processedInCurrentBuild(String sourceType) {
		return this.processedSourceTypes.contains(sourceType);
	}

}
