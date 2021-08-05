/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;
import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Buildpack metadata information.
 *
 * @author Scott Frederick
 */
final class BuildpackMetadata extends MappedObject {

	private static final String LABEL_NAME = "io.buildpacks.buildpackage.metadata";

	private final String id;

	private final String version;

	private final String homepage;

	private BuildpackMetadata(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.id = valueAt("/id", String.class);
		this.version = valueAt("/version", String.class);
		this.homepage = valueAt("/homepage", String.class);
	}

	/**
	 * Return the buildpack ID.
	 * @return the ID
	 */
	String getId() {
		return this.id;
	}

	/**
	 * Return the buildpack version.
	 * @return the version
	 */
	String getVersion() {
		return this.version;
	}

	/**
	 * Return the buildpack homepage address.
	 * @return the homepage
	 */
	String getHomepage() {
		return this.homepage;
	}

	/**
	 * Factory method to extract {@link BuildpackMetadata} from an image.
	 * @param image the source image
	 * @return the builder metadata
	 * @throws IOException on IO error
	 */
	static BuildpackMetadata fromImage(Image image) throws IOException {
		Assert.notNull(image, "Image must not be null");
		return fromImageConfig(image.getConfig());
	}

	/**
	 * Factory method to extract {@link BuildpackMetadata} from image config.
	 * @param imageConfig the source image config
	 * @return the builder metadata
	 * @throws IOException on IO error
	 */
	static BuildpackMetadata fromImageConfig(ImageConfig imageConfig) throws IOException {
		Assert.notNull(imageConfig, "ImageConfig must not be null");
		String json = imageConfig.getLabels().get(LABEL_NAME);
		Assert.notNull(json, () -> "No '" + LABEL_NAME + "' label found in image config labels '"
				+ StringUtils.collectionToCommaDelimitedString(imageConfig.getLabels().keySet()) + "'");
		return fromJson(json);
	}

	/**
	 * Factory method create {@link BuildpackMetadata} from JSON.
	 * @param json the source JSON
	 * @return the builder metadata
	 * @throws IOException on IO error
	 */
	static BuildpackMetadata fromJson(String json) throws IOException {
		return fromJson(SharedObjectMapper.get().readTree(json));
	}

	/**
	 * Factory method create {@link BuildpackMetadata} from JSON.
	 * @param node the source JSON
	 * @return the builder metadata
	 */
	static BuildpackMetadata fromJson(JsonNode node) {
		return new BuildpackMetadata(node);
	}

}
