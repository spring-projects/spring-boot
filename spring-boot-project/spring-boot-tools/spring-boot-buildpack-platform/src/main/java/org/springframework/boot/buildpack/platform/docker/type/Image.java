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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;

/**
 * Image details as returned from {@code Docker inspect}.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class Image extends MappedObject {

	private final List<String> digests;

	private final ImageConfig config;

	private final List<LayerId> layers;

	private final String os;

	private final String created;

	Image(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.digests = getDigests(getNode().at("/RepoDigests"));
		this.config = new ImageConfig(getNode().at("/Config"));
		this.layers = extractLayers(valueAt("/RootFS/Layers", String[].class));
		this.os = valueAt("/Os", String.class);
		this.created = valueAt("/Created", String.class);
	}

	private List<String> getDigests(JsonNode node) {
		if (node.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> digests = new ArrayList<>();
		node.forEach((child) -> digests.add(child.asText()));
		return Collections.unmodifiableList(digests);
	}

	private List<LayerId> extractLayers(String[] layers) {
		if (layers == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(layers).map(LayerId::of).toList();
	}

	/**
	 * Return the digests of the image.
	 * @return the image digests
	 */
	public List<String> getDigests() {
		return this.digests;
	}

	/**
	 * Return image config information.
	 * @return the image config
	 */
	public ImageConfig getConfig() {
		return this.config;
	}

	/**
	 * Return the layer IDs contained in the image.
	 * @return the layer IDs.
	 */
	public List<LayerId> getLayers() {
		return this.layers;
	}

	/**
	 * Return the OS of the image.
	 * @return the image OS
	 */
	public String getOs() {
		return (this.os != null) ? this.os : "linux";
	}

	/**
	 * Return the created date of the image.
	 * @return the image created date
	 */
	public String getCreated() {
		return this.created;
	}

	/**
	 * Create a new {@link Image} instance from the specified JSON content.
	 * @param content the JSON content
	 * @return a new {@link Image} instance
	 * @throws IOException on IO error
	 */
	public static Image of(InputStream content) throws IOException {
		return of(content, Image::new);
	}

}
