/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Interface to provide information about layers to the {@link Repackager}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.3.0
 * @see Layer
 */
public interface Layers extends Iterable<Layer> {

	/**
	 * The default layer resolver.
	 */
	Layers IMPLICIT = new ImplicitLayerResolver();

	/**
	 * Return the jar layers in the order that they should be added (starting with the
	 * least frequently changed layer).
	 * @return the layers iterator
	 */
	@Override
	Iterator<Layer> iterator();

	/**
	 * Return a stream of the jar layers in the order that they should be added (starting
	 * with the least frequently changed layer).
	 * @return the layers stream
	 */
	Stream<Layer> stream();

	/**
	 * Return the layer that contains the given resource name.
	 * @param applicationResource the name of an application resource (for example a
	 * {@code .class} file).
	 * @return the layer that contains the resource (must never be {@code null})
	 */
	Layer getLayer(String applicationResource);

	/**
	 * Return the layer that contains the given library.
	 * @param library the library to consider
	 * @return the layer that contains the resource (must never be {@code null})
	 */
	Layer getLayer(Library library);

}
