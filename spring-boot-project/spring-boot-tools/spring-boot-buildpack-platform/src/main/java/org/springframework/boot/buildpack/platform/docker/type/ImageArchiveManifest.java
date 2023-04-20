/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;

/**
 * Image archive manifest information.
 *
 * @author Scott Frederick
 * @since 2.7.10
 */
public class ImageArchiveManifest extends MappedObject {

	private final List<ManifestEntry> entries = new ArrayList<>();

	protected ImageArchiveManifest(JsonNode node) {
		super(node, MethodHandles.lookup());
		getNode().elements().forEachRemaining((element) -> this.entries.add(ManifestEntry.of(element)));
	}

	/**
	 * Return the entries contained in the manifest.
	 * @return the manifest entries
	 */
	public List<ManifestEntry> getEntries() {
		return this.entries;
	}

	/**
	 * Create an {@link ImageArchiveManifest} from the provided JSON input stream.
	 * @param content the JSON input stream
	 * @return a new {@link ImageArchiveManifest} instance
	 * @throws IOException on IO error
	 */
	public static ImageArchiveManifest of(InputStream content) throws IOException {
		return of(content, ImageArchiveManifest::new);
	}

	public static class ManifestEntry extends MappedObject {

		private final List<String> layers;

		protected ManifestEntry(JsonNode node) {
			super(node, MethodHandles.lookup());
			this.layers = extractLayers();
		}

		/**
		 * Return the collection of layer IDs from a section of the manifest.
		 * @return a collection of layer IDs
		 */
		public List<String> getLayers() {
			return this.layers;
		}

		static ManifestEntry of(JsonNode node) {
			return new ManifestEntry(node);
		}

		@SuppressWarnings("unchecked")
		private List<String> extractLayers() {
			List<String> layers = valueAt("/Layers", List.class);
			if (layers == null) {
				return Collections.emptyList();
			}
			return layers;
		}

	}

}
