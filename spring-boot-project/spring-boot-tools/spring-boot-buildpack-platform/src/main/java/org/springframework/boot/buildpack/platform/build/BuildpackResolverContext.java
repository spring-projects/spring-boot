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
