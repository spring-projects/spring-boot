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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;
import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.boot.buildpack.platform.json.SharedJsonMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Builder metadata information.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class BuilderMetadata extends MappedObject {

	private static final String LABEL_NAME = "io.buildpacks.builder.metadata";

	private static final String[] EMPTY_MIRRORS = {};

	private final Stack stack;

	private final List<RunImage> runImages;

	private final Lifecycle lifecycle;

	private final CreatedBy createdBy;

	private final List<BuildpackMetadata> buildpacks;

	BuilderMetadata(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.stack = extractStack();
		this.runImages = childrenAt("/images", RunImage::new);
		this.lifecycle = extractLifecycle();
		this.createdBy = extractCreatedBy();
		this.buildpacks = extractBuildpacks(getNode().at("/buildpacks"));
	}

	private CreatedBy extractCreatedBy() {
		CreatedBy result = valueAt("/createdBy", CreatedBy.class);
		Assert.state(result != null, "'result' must not be null");
		return result;
	}

	private Lifecycle extractLifecycle() {
		Lifecycle result = valueAt("/lifecycle", Lifecycle.class);
		Assert.state(result != null, "'result' must not be null");
		return result;
	}

	private Stack extractStack() {
		Stack result = valueAt("/stack", Stack.class);
		Assert.state(result != null, "'result' must not be null");
		return result;
	}

	private List<BuildpackMetadata> extractBuildpacks(JsonNode node) {
		if (node.isEmpty()) {
			return Collections.emptyList();
		}
		List<BuildpackMetadata> entries = new ArrayList<>();
		node.forEach((child) -> entries.add(BuildpackMetadata.fromJson(child)));
		return entries;
	}

	/**
	 * Return stack metadata.
	 * @return the stack metadata
	 */
	Stack getStack() {
		return this.stack;
	}

	/**
	 * Return run images metadata.
	 * @return the run images metadata
	 */
	List<RunImage> getRunImages() {
		return this.runImages;
	}

	/**
	 * Return lifecycle metadata.
	 * @return the lifecycle metadata
	 */
	Lifecycle getLifecycle() {
		return this.lifecycle;
	}

	/**
	 * Return information about who created the builder.
	 * @return the created by metadata
	 */
	CreatedBy getCreatedBy() {
		return this.createdBy;
	}

	/**
	 * Return the buildpacks that are bundled in the builder.
	 * @return the buildpacks
	 */
	List<BuildpackMetadata> getBuildpacks() {
		return this.buildpacks;
	}

	/**
	 * Create an updated copy of this metadata.
	 * @param update consumer to apply updates
	 * @return an updated metadata instance
	 */
	BuilderMetadata copy(Consumer<Update> update) {
		return new Update(this).run(update);
	}

	/**
	 * Attach this metadata to the given update callback.
	 * @param update the update used to attach the metadata
	 */
	void attachTo(ImageConfig.Update update) {
		try {
			String json = SharedJsonMapper.get().writeValueAsString(getNode());
			update.withLabel(LABEL_NAME, json);
		}
		catch (JacksonException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Factory method to extract {@link BuilderMetadata} from an image.
	 * @param image the source image
	 * @return the builder metadata
	 * @throws IOException on IO error
	 */
	static BuilderMetadata fromImage(Image image) throws IOException {
		Assert.notNull(image, "'image' must not be null");
		return fromImageConfig(image.getConfig());
	}

	/**
	 * Factory method to extract {@link BuilderMetadata} from image config.
	 * @param imageConfig the image config
	 * @return the builder metadata
	 * @throws IOException on IO error
	 */
	static BuilderMetadata fromImageConfig(ImageConfig imageConfig) throws IOException {
		Assert.notNull(imageConfig, "'imageConfig' must not be null");
		String json = imageConfig.getLabels().get(LABEL_NAME);
		Assert.state(json != null, () -> "No '" + LABEL_NAME + "' label found in image config labels '"
				+ StringUtils.collectionToCommaDelimitedString(imageConfig.getLabels().keySet()) + "'");
		return fromJson(json);
	}

	/**
	 * Factory method create {@link BuilderMetadata} from some JSON.
	 * @param json the source JSON
	 * @return the builder metadata
	 * @throws IOException on IO error
	 */
	static BuilderMetadata fromJson(String json) throws IOException {
		return new BuilderMetadata(SharedJsonMapper.get().readTree(json));
	}

	/**
	 * Stack metadata.
	 */
	interface Stack {

		/**
		 * Return run image metadata.
		 * @return the run image metadata
		 */
		RunImage getRunImage();

		/**
		 * Run image metadata.
		 */
		interface RunImage {

			/**
			 * Return the builder image reference.
			 * @return the image reference
			 */
			String getImage();

			/**
			 * Return stack mirrors.
			 * @return the stack mirrors
			 */
			default String[] getMirrors() {
				return EMPTY_MIRRORS;
			}

		}

	}

	static class RunImage extends MappedObject {

		private final String image;

		private final List<String> mirrors;

		/**
		 * Create a new {@link MappedObject} instance.
		 * @param node the source node
		 */
		RunImage(JsonNode node) {
			super(node, MethodHandles.lookup());
			this.image = extractImage();
			this.mirrors = childrenAt("/mirrors", JsonNode::asString);
		}

		private String extractImage() {
			String result = valueAt("/image", String.class);
			Assert.state(result != null, "'result' must not be null");
			return result;
		}

		String getImage() {
			return this.image;
		}

		List<String> getMirrors() {
			return this.mirrors;
		}

	}

	/**
	 * Lifecycle metadata.
	 */
	interface Lifecycle {

		/**
		 * Return the lifecycle version.
		 * @return the lifecycle version
		 */
		String getVersion();

		/**
		 * Return the default API versions.
		 * @return the API versions
		 */
		Api getApi();

		/**
		 * Return the supported API versions.
		 * @return the API versions
		 */
		Apis getApis();

		/**
		 * Default API versions.
		 */
		interface Api {

			/**
			 * Return the default buildpack API version.
			 * @return the buildpack version
			 */
			String getBuildpack();

			/**
			 * Return the default platform API version.
			 * @return the platform version
			 */
			String getPlatform();

		}

		/**
		 * Supported API versions.
		 */
		interface Apis {

			/**
			 * Return the supported buildpack API versions.
			 * @return the buildpack versions
			 */
			default String @Nullable [] getBuildpack() {
				return valueAt(this, "/buildpack/supported", String[].class);
			}

			/**
			 * Return the supported platform API versions.
			 * @return the platform versions
			 */
			default String @Nullable [] getPlatform() {
				return valueAt(this, "/platform/supported", String[].class);
			}

		}

	}

	/**
	 * Created-by metadata.
	 */
	interface CreatedBy {

		/**
		 * Return the name of the creator.
		 * @return the creator name
		 */
		String getName();

		/**
		 * Return the version of the creator.
		 * @return the creator version
		 */
		String getVersion();

	}

	/**
	 * Update class used to change data when creating a copy.
	 */
	static final class Update {

		private final ObjectNode copy;

		private Update(BuilderMetadata source) {
			this.copy = (ObjectNode) source.getNode().deepCopy();
		}

		private BuilderMetadata run(Consumer<Update> update) {
			update.accept(this);
			return new BuilderMetadata(this.copy);
		}

		/**
		 * Update the builder meta-data with a specific created by section.
		 * @param name the name of the creator
		 * @param version the version of the creator
		 */
		void withCreatedBy(String name, String version) {
			ObjectNode createdBy = (ObjectNode) this.copy.at("/createdBy");
			if (createdBy == null) {
				createdBy = this.copy.putObject("createdBy");
			}
			createdBy.put("name", name);
			createdBy.put("version", version);
		}

	}

}
