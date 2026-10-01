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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;

/**
 * Container for {@link MetadataCollector}. Usually, either metadata for the whole module
 * or metadata for types is generated. This makes sure to record types that have been
 * processed and determine if previous metadata should be merged.
 *
 * @author Stephane Nicoll
 */
class MetadataCollectors {

	private final ProcessingContext context;

	private final MetadataStore metadataStore;

	private final MetadataCollector metadataCollector;

	private final Set<String> processedSourceTypes = new HashSet<>();

	private final Map<TypeDeclaration, MetadataCollector> metadataTypeCollectors = new HashMap<>();

	MetadataCollectors(ProcessingContext context) {
		this.context = context;
		this.metadataStore = new MetadataStore(context);
		this.metadataCollector = new MetadataCollector(this::shouldBeMerged, this.metadataStore.readMetadata());
	}

	void processing(ProcessingRound round) {
		for (TypeDeclaration element : round.getRootTypes()) {
			this.processedSourceTypes.add(element.getQualifiedName());
		}
	}

	MetadataCollector getModuleMetadataCollector() {
		return this.metadataCollector;
	}

	MetadataCollector getMetadataCollector(TypeDeclaration element) {
		return this.metadataTypeCollectors.computeIfAbsent(element,
				(ignored) -> new MetadataCollector(this::shouldBeMerged, this.metadataStore.readMetadata(element)));
	}

	Set<TypeDeclaration> getSourceTypes() {
		return this.metadataTypeCollectors.keySet();
	}

	private boolean shouldBeMerged(ItemMetadata itemMetadata) {
		String sourceType = itemMetadata.getSourceType();
		return (sourceType != null && !deletedInCurrentBuild(sourceType) && !processedInCurrentBuild(sourceType));
	}

	private boolean deletedInCurrentBuild(String sourceType) {
		return this.context.getTypeDeclaration(sourceType.replace('$', '.')) == null;
	}

	private boolean processedInCurrentBuild(String sourceType) {
		return this.processedSourceTypes.contains(sourceType);
	}

}
