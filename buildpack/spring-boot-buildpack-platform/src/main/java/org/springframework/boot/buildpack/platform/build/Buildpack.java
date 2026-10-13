/*
 * Copyright 2012-present the original author or authors.
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
