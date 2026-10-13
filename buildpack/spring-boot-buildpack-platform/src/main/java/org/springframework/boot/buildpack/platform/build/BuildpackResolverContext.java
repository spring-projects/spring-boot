/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;

/**
 * Context passed to a {@link BuildpackResolver}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
interface BuildpackResolverContext {

	List<BuildpackMetadata> getBuildpackMetadata();

	BuildpackLayersMetadata getBuildpackLayersMetadata();

	/**
	 * Retrieve an image.
	 * @param reference the image reference
	 * @param type the type of image
	 * @return the retrieved image
	 * @throws IOException on IO error
	 */
	Image fetchImage(ImageReference reference, ImageType type) throws IOException;

	/**
	 * Export the layers of an image.
	 * @param reference the reference to export
	 * @param exports a consumer to receive the layers (contents can only be accessed
	 * during the callback)
	 * @throws IOException on IO error
	 */
	void exportImageLayers(ImageReference reference, IOBiConsumer<String, TarArchive> exports) throws IOException;

}
