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
