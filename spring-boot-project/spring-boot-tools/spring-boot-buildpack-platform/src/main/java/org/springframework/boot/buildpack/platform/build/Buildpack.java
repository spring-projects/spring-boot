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

import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.io.IOConsumer;

/**
 * A Buildpack that should be invoked by the builder during image building.
 *
 * @author Scott Frederick
 * @see BuildpackResolver
 */
interface Buildpack {

	/**
	 * Return the coordinates of the builder.
	 * @return the builder coordinates
	 */
	BuildpackCoordinates getCoordinates();

	/**
	 * Apply the necessary buildpack layers.
	 * @param layers a consumer that should accept the layers
	 * @throws IOException on IO error
	 */
	void apply(IOConsumer<Layer> layers) throws IOException;

}
