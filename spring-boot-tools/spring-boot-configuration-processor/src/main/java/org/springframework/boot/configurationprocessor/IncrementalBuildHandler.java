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

package org.springframework.boot.configurationprocessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * {@code BuildHandler} that provides incremental build support by merging the metadata
 * from the current incremental build with any existing metadata.
 *
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @since 1.2.2
 */
public class IncrementalBuildHandler extends StandardBuildHandler {

	private final Set<String> processedSourceTypes = new HashSet<String>();

	private final ProcessingEnvironment processingEnvironment;

	private final ConfigurationMetadata existingMetadata;

	private final TypeUtils typeUtils;

	/**
	 * Creates a new {@code IncrementalBuildTracker} that will merge the metadata produced
	 * by an incremental build with the given {@code existingMetadata}.
	 *
	 * @param processingEnvironment The processing environment of the build
	 * @param existingMetadata The existing metadata
	 */
	public IncrementalBuildHandler(ProcessingEnvironment processingEnvironment,
			ConfigurationMetadata existingMetadata) {
		this.existingMetadata = existingMetadata;
		this.processingEnvironment = processingEnvironment;
		this.typeUtils = new TypeUtils(processingEnvironment);
	}

	@Override
	public void processing(RoundEnvironment environment) {
		for (Element element : environment.getRootElements()) {
			markAsProcessed(element);
		}
	}

	@Override
	public ConfigurationMetadata produceMetadata() {
		ConfigurationMetadata metadata = super.produceMetadata();
		mergeExistingMetadata(metadata);
		return metadata;
	}

	private void markAsProcessed(Element element) {
		if (element instanceof TypeElement) {
			this.processedSourceTypes.add(this.typeUtils.getType(element));
		}
	}

	private void mergeExistingMetadata(ConfigurationMetadata metadata) {
		List<ItemMetadata> items = this.existingMetadata.getItems();
		for (ItemMetadata oldItem : items) {
			if (shouldBeMerged(oldItem)) {
				metadata.add(oldItem);
			}
		}
	}

	private boolean shouldBeMerged(ItemMetadata itemMetadata) {
		String sourceType = itemMetadata.getSourceType();
		if (sourceType == null || deletedInCurrentBuild(sourceType)
				|| processedInCurrentBuild(sourceType)) {
			return false;
		}
		return true;
	}

	private boolean deletedInCurrentBuild(String sourceType) {
		return this.processingEnvironment.getElementUtils().getTypeElement(sourceType) == null;
	}

	private boolean processedInCurrentBuild(String sourceType) {
		return this.processedSourceTypes.contains(sourceType);
	}

}
