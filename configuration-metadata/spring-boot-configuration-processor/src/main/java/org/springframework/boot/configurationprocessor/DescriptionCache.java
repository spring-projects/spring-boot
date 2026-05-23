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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;

/**
 * Cache for property descriptions that persists across incremental builds. Stores
 * metadata in a location outside of {@code CLASS_OUTPUT} so that Gradle's incremental
 * compilation does not delete it.
 *
 * @author Agustin Palazzo
 * @see ConfigurationMetadataAnnotationProcessor
 */
class DescriptionCache {

	private final File cacheFile;

	private ConfigurationMetadata cachedMetadata;

	DescriptionCache(String cacheFilePath) {
		this.cacheFile = new File(cacheFilePath);
		this.cachedMetadata = read();
	}

	/**
	 * Look up a cached description for the given property name.
	 * @param propertyName the fully qualified property name
	 * @return the cached description or {@code null}
	 */
	String getDescription(String propertyName) {
		if (this.cachedMetadata == null) {
			return null;
		}
		for (ItemMetadata item : this.cachedMetadata.getItems()) {
			if (item.isOfItemType(ItemMetadata.ItemType.PROPERTY) && propertyName.equals(item.getName())) {
				return item.getDescription();
			}
		}
		return null;
	}

	/**
	 * Replace the cache with the given metadata. After
	 * {@link ConfigurationMetadataAnnotationProcessor#fillCachedDescriptions} has
	 * restored descriptions from the cache, the metadata is the complete picture of the
	 * current build. Replacing (instead of merging) ensures that entries for deleted or
	 * de-annotated types are automatically pruned.
	 * @param metadata the current build's metadata with descriptions already filled
	 */
	void update(ConfigurationMetadata metadata) {
		write(metadata);
		this.cachedMetadata = new ConfigurationMetadata(metadata);
	}

	private ConfigurationMetadata read() {
		if (!this.cacheFile.exists()) {
			return null;
		}
		try (FileInputStream in = new FileInputStream(this.cacheFile)) {
			return new JsonMarshaller().read(in);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private void write(ConfigurationMetadata metadata) {
		this.cacheFile.getParentFile().mkdirs();
		try (FileOutputStream out = new FileOutputStream(this.cacheFile)) {
			new JsonMarshaller().write(metadata, out);
		}
		catch (IOException ex) {
			// Cache write failure is non-fatal
		}
	}

}
