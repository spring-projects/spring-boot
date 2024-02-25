/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.Map;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.Owner;

/**
 * A short-lived builder that is created for each {@link Lifecycle} run.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class EphemeralBuilder {

	static final String BUILDER_FOR_LABEL_NAME = "org.springframework.boot.builderFor";

	private final BuildOwner buildOwner;

	private final BuilderMetadata builderMetadata;

	private final ImageArchive archive;

	private final Creator creator;

	/**
	 * Create a new {@link EphemeralBuilder} instance.
	 * @param buildOwner the build owner
	 * @param builderImage the base builder image
	 * @param targetImage the image being built
	 * @param builderMetadata the builder metadata
	 * @param creator the builder creator
	 * @param env the builder env
	 * @param buildpacks an optional set of buildpacks to apply
	 * @throws IOException on IO error
	 */
	EphemeralBuilder(BuildOwner buildOwner, Image builderImage, ImageReference targetImage,
			BuilderMetadata builderMetadata, Creator creator, Map<String, String> env, Buildpacks buildpacks)
			throws IOException {
		ImageReference name = ImageReference.random("pack.local/builder/").inTaggedForm();
		this.buildOwner = buildOwner;
		this.creator = creator;
		this.builderMetadata = builderMetadata.copy(this::updateMetadata);
		this.archive = ImageArchive.from(builderImage, (update) -> {
			update.withUpdatedConfig(this.builderMetadata::attachTo);
			update.withUpdatedConfig((config) -> config.withLabel(BUILDER_FOR_LABEL_NAME, targetImage.toString()));
			update.withTag(name);
			if (env != null && !env.isEmpty()) {
				update.withNewLayer(getEnvLayer(env));
			}
			if (buildpacks != null) {
				buildpacks.apply(update::withNewLayer);
			}
		});
	}

	/**
     * Updates the metadata of the EphemeralBuilder with the provided update.
     * 
     * @param update the update containing the new metadata values
     */
    private void updateMetadata(BuilderMetadata.Update update) {
		update.withCreatedBy(this.creator.getName(), this.creator.getVersion());
	}

	/**
     * Returns a Layer object representing the environment variables specified in the given map.
     * The Layer object is created by iterating over the entries in the map and creating a file
     * for each entry in the "/platform/env/" directory of the layer layout. The name of each file
     * is constructed by appending the key of the entry to the "/platform/env/" directory path.
     * The content of each file is set to the value of the entry, or an empty string if the value is null.
     * 
     * @param env the map containing the environment variables
     * @return a Layer object representing the environment variables
     * @throws IOException if an I/O error occurs while creating the Layer object
     */
    private Layer getEnvLayer(Map<String, String> env) throws IOException {
		return Layer.of((layout) -> {
			for (Map.Entry<String, String> entry : env.entrySet()) {
				String name = "/platform/env/" + entry.getKey();
				Content content = Content.of((entry.getValue() != null) ? entry.getValue() : "");
				layout.file(name, Owner.ROOT, content);
			}
		});
	}

	/**
	 * Return the name of this archive as tagged in Docker.
	 * @return the ephemeral builder name
	 */
	ImageReference getName() {
		return this.archive.getTag();
	}

	/**
	 * Return the build owner that should be used for written content.
	 * @return the builder owner
	 */
	Owner getBuildOwner() {
		return this.buildOwner;
	}

	/**
	 * Return the builder meta-data that was used to create this ephemeral builder.
	 * @return the builder meta-data
	 */
	BuilderMetadata getBuilderMetadata() {
		return this.builderMetadata;
	}

	/**
	 * Return the contents of ephemeral builder for passing to Docker.
	 * @return the ephemeral builder archive
	 */
	ImageArchive getArchive() {
		return this.archive;
	}

	/**
     * Returns a string representation of the tag in the archive.
     * 
     * @return the string representation of the tag in the archive
     */
    @Override
	public String toString() {
		return this.archive.getTag().toString();
	}

}
