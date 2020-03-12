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

package org.springframework.boot.loader.tools.layer.library;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Library;
import org.springframework.util.Assert;

/**
 * A {@link LibraryStrategy} with custom filters.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class FilteredLibraryStrategy implements LibraryStrategy {

	private final Layer layer;

	private final List<LibraryFilter> filters = new ArrayList<>();

	public FilteredLibraryStrategy(String layer, List<LibraryFilter> filters) {
		Assert.notEmpty(filters, "Filters should not be empty for custom strategy.");
		this.layer = new Layer(layer);
		this.filters.addAll(filters);
	}

	public Layer getLayer() {
		return this.layer;
	}

	@Override
	public Layer getMatchingLayer(Library library) {
		boolean isIncluded = false;
		for (LibraryFilter filter : this.filters) {
			if (filter.isLibraryExcluded(library)) {
				return null;
			}
			if (!isIncluded && filter.isLibraryIncluded(library)) {
				isIncluded = true;
			}
		}
		return (isIncluded) ? this.layer : null;
	}

}
