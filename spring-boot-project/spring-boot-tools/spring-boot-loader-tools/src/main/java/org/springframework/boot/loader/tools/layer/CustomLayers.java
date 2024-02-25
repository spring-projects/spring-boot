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

package org.springframework.boot.loader.tools.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Layers;
import org.springframework.boot.loader.tools.Library;
import org.springframework.util.Assert;

/**
 * Custom {@link Layers} implementation where layer content is selected by the user.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.3.0
 */
public class CustomLayers implements Layers {

	private final List<Layer> layers;

	private final List<ContentSelector<String>> applicationSelectors;

	private final List<ContentSelector<Library>> librarySelectors;

	/**
     * Constructs a new CustomLayers object with the given layers, application selectors, and library selectors.
     * 
     * @param layers the list of layers to be used
     * @param applicationSelectors the list of content selectors for application
     * @param librarySelectors the list of content selectors for library
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public CustomLayers(List<Layer> layers, List<ContentSelector<String>> applicationSelectors,
			List<ContentSelector<Library>> librarySelectors) {
		Assert.notNull(layers, "Layers must not be null");
		Assert.notNull(applicationSelectors, "ApplicationSelectors must not be null");
		validateSelectorLayers(applicationSelectors, layers);
		Assert.notNull(librarySelectors, "LibrarySelectors must not be null");
		validateSelectorLayers(librarySelectors, layers);
		this.layers = new ArrayList<>(layers);
		this.applicationSelectors = new ArrayList<>(applicationSelectors);
		this.librarySelectors = new ArrayList<>(librarySelectors);
	}

	/**
     * Validates the layers of the given content selectors against the provided list of layers.
     * 
     * @param selectors the list of content selectors to validate
     * @param layers the list of layers to validate against
     * @param <T> the type of the content selectors
     * @throws IllegalArgumentException if any of the content selectors have invalid layers
     */
    private static <T> void validateSelectorLayers(List<ContentSelector<T>> selectors, List<Layer> layers) {
		for (ContentSelector<?> selector : selectors) {
			validateSelectorLayers(selector, layers);
		}
	}

	/**
     * Validates the selector layers by checking if the given selector's layer is present in the list of layers.
     * 
     * @param selector the content selector to validate
     * @param layers the list of layers to check against
     * @throws IllegalStateException if the selector's layer is missing or not found in the list of layers
     */
    private static void validateSelectorLayers(ContentSelector<?> selector, List<Layer> layers) {
		Layer layer = selector.getLayer();
		Assert.state(layer != null, "Missing content selector layer");
		Assert.state(layers.contains(layer),
				() -> "Content selector layer '" + selector.getLayer() + "' not found in " + layers);
	}

	/**
     * Returns an iterator over the elements in this CustomLayers object in proper sequence.
     *
     * @return an iterator over the elements in this CustomLayers object in proper sequence
     */
    @Override
	public Iterator<Layer> iterator() {
		return this.layers.iterator();
	}

	/**
     * Returns a sequential Stream with the layers of this CustomLayers object as its source.
     *
     * @return a sequential Stream of Layer objects
     */
    @Override
	public Stream<Layer> stream() {
		return this.layers.stream();
	}

	/**
     * Retrieves the layer associated with the given resource name.
     * 
     * @param resourceName the name of the resource
     * @return the layer associated with the resource name
     * @throws IllegalArgumentException if the resource name is null or empty
     */
    @Override
	public Layer getLayer(String resourceName) {
		return selectLayer(resourceName, this.applicationSelectors, () -> "Resource '" + resourceName + "'");
	}

	/**
     * Returns the layer for the given library.
     * 
     * @param library the library for which to get the layer
     * @return the layer for the given library
     * @throws IllegalArgumentException if the library is null
     */
    @Override
	public Layer getLayer(Library library) {
		return selectLayer(library, this.librarySelectors, () -> "Library '" + library.getName() + "'");
	}

	/**
     * Selects the layer for the given item based on the provided content selectors.
     * 
     * @param <T>       the type of the item
     * @param item      the item to select the layer for
     * @param selectors the list of content selectors to use for layer selection
     * @param name      the supplier for the name of the item
     * @return the layer selected for the item
     * @throws IllegalStateException if the item does not match any layer
     */
    private <T> Layer selectLayer(T item, List<ContentSelector<T>> selectors, Supplier<String> name) {
		for (ContentSelector<T> selector : selectors) {
			if (selector.contains(item)) {
				return selector.getLayer();
			}
		}
		throw new IllegalStateException(name.get() + " did not match any layer");
	}

}
