/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Container for {@link MetadataCollector}. Usually, either metadata for the whole module
 * or metadata for types is generated. This makes sure to record types that have been
 * processed and determine if previous metadata should be merged.
 *
 * @author Stephane Nicoll
 */
class MetadataCollectors {

	private final ProcessingEnvironment processingEnvironment;

	private final TypeUtils typeUtils;

	private final MetadataStore metadataStore;

	private final MetadataCollector metadataCollector;

	private final Set<String> processedSourceTypes = new HashSet<>();

	private final Map<TypeElement, MetadataCollector> metadataTypeCollectors = new HashMap<>();

	MetadataCollectors(ProcessingEnvironment processingEnvironment, TypeUtils typeUtils) {
		this.processingEnvironment = processingEnvironment;
		this.typeUtils = typeUtils;
		this.metadataStore = new MetadataStore(this.processingEnvironment, this.typeUtils);
		this.metadataCollector = new MetadataCollector(this::shouldBeMerged, this.metadataStore.readMetadata());
	}

	void processing(RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getRootElements()) {
			if (element instanceof TypeElement) {
				this.processedSourceTypes.add(this.typeUtils.getQualifiedName(element));
			}
		}
	}

	MetadataCollector getModuleMetadataCollector() {
		return this.metadataCollector;
	}

	MetadataCollector getMetadataCollector(TypeElement element) {
		return this.metadataTypeCollectors.computeIfAbsent(element,
				(ignored) -> new MetadataCollector(this::shouldBeMerged, this.metadataStore.readMetadata(element)));
	}

	Set<TypeElement> getSourceTypes() {
		return this.metadataTypeCollectors.keySet();
	}

	private boolean shouldBeMerged(ItemMetadata itemMetadata) {
		String sourceType = itemMetadata.getSourceType();
		return (sourceType != null && !deletedInCurrentBuild(sourceType) && !processedInCurrentBuild(sourceType));
	}

	private boolean deletedInCurrentBuild(String sourceType) {
		return this.processingEnvironment.getElementUtils().getTypeElement(sourceType.replace('$', '.')) == null;
	}

	private boolean processedInCurrentBuild(String sourceType) {
		return this.processedSourceTypes.contains(sourceType);
	}

}
