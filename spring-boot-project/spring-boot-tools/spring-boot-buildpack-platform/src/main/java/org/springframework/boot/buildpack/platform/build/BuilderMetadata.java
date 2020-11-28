/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;
import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
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

	private final Lifecycle lifecycle;

	private final CreatedBy createdBy;

	BuilderMetadata(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.stack = valueAt("/stack", Stack.class);
		this.lifecycle = valueAt("/lifecycle", Lifecycle.class);
		this.createdBy = valueAt("/createdBy", CreatedBy.class);
	}

	/**
	 * Return stack metadata.
	 * @return the stack metadata
	 */
	Stack getStack() {
		return this.stack;
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
			String json = SharedObjectMapper.get().writeValueAsString(getNode());
			update.withLabel(LABEL_NAME, json);
		}
		catch (JsonProcessingException ex) {
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
		Assert.notNull(image, "Image must not be null");
		return fromImageConfig(image.getConfig());
	}

	/**
	 * Factory method to extract {@link BuilderMetadata} from image config.
	 * @param imageConfig the image config
	 * @return the builder metadata
	 * @throws IOException on IO error
	 */
	static BuilderMetadata fromImageConfig(ImageConfig imageConfig) throws IOException {
		Assert.notNull(imageConfig, "ImageConfig must not be null");
		String json = imageConfig.getLabels().get(LABEL_NAME);
		Assert.notNull(json, () -> "No '" + LABEL_NAME + "' label found in image config labels '"
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
		return new BuilderMetadata(SharedObjectMapper.get().readTree(json));
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
			default String[] getBuildpack() {
				return valueAt(this, "/buildpack/supported", String[].class);
			}

			/**
			 * Return the supported platform API versions.
			 * @return the platform versions
			 */
			default String[] getPlatform() {
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
			this.copy = source.getNode().deepCopy();
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
