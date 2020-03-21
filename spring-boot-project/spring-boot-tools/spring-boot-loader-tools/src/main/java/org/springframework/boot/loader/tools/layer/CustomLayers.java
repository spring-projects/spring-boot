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

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Layers;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.layer.application.ResourceStrategy;
import org.springframework.boot.loader.tools.layer.library.LibraryStrategy;

/**
 * Implementation of {@link Layers} representing user-provided layers.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class CustomLayers implements Layers {

	private final List<Layer> layers;

	private final List<ResourceStrategy> resourceStrategies;

	private final List<LibraryStrategy> libraryStrategies;

	public CustomLayers(List<Layer> layers, List<ResourceStrategy> resourceStrategies,
			List<LibraryStrategy> libraryStrategies) {
		this.layers = new ArrayList<>(layers);
		this.resourceStrategies = new ArrayList<>(resourceStrategies);
		this.libraryStrategies = new ArrayList<>(libraryStrategies);
	}

	@Override
	public Iterator<Layer> iterator() {
		return this.layers.iterator();
	}

	@Override
	public Layer getLayer(String resourceName) {
		for (ResourceStrategy strategy : this.resourceStrategies) {
			Layer matchingLayer = strategy.getMatchingLayer(resourceName);
			if (matchingLayer != null) {
				validateLayerName(matchingLayer, "Resource '" + resourceName + "'");
				return matchingLayer;
			}
		}
		throw new IllegalStateException("Resource '" + resourceName + "' did not match any layer.");
	}

	@Override
	public Layer getLayer(Library library) {
		for (LibraryStrategy strategy : this.libraryStrategies) {
			Layer matchingLayer = strategy.getMatchingLayer(library);
			if (matchingLayer != null) {
				validateLayerName(matchingLayer, "Library '" + library.getName() + "'");
				return matchingLayer;
			}
		}
		throw new IllegalStateException("Library '" + library.getName() + "' did not match any layer.");
	}

	private void validateLayerName(Layer layer, String nameText) {
		if (!this.layers.contains(layer)) {
			throw new IllegalStateException(nameText + " matched a layer '" + layer
					+ "' that is not included in the configured layers " + this.layers + ".");
		}
	}

}
