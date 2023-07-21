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

	private static <T> void validateSelectorLayers(List<ContentSelector<T>> selectors, List<Layer> layers) {
		for (ContentSelector<?> selector : selectors) {
			validateSelectorLayers(selector, layers);
		}
	}

	private static void validateSelectorLayers(ContentSelector<?> selector, List<Layer> layers) {
		Layer layer = selector.getLayer();
		Assert.state(layer != null, "Missing content selector layer");
		Assert.state(layers.contains(layer),
				() -> "Content selector layer '" + selector.getLayer() + "' not found in " + layers);
	}

	@Override
	public Iterator<Layer> iterator() {
		return this.layers.iterator();
	}

	@Override
	public Stream<Layer> stream() {
		return this.layers.stream();
	}

	@Override
	public Layer getLayer(String resourceName) {
		return selectLayer(resourceName, this.applicationSelectors, () -> "Resource '" + resourceName + "'");
	}

	@Override
	public Layer getLayer(Library library) {
		return selectLayer(library, this.librarySelectors, () -> "Library '" + library.getName() + "'");
	}

	private <T> Layer selectLayer(T item, List<ContentSelector<T>> selectors, Supplier<String> name) {
		for (ContentSelector<T> selector : selectors) {
			if (selector.contains(item)) {
				return selector.getLayer();
			}
		}
		throw new IllegalStateException(name.get() + " did not match any layer");
	}

}
