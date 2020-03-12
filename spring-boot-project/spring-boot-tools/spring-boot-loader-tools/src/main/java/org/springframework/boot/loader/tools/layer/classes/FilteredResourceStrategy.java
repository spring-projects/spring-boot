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

package org.springframework.boot.loader.tools.layer.classes;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.util.Assert;

/**
 * A {@link ResourceStrategy} with custom filters.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class FilteredResourceStrategy implements ResourceStrategy {

	private final Layer layer;

	private final List<ResourceFilter> filters = new ArrayList<>();

	public FilteredResourceStrategy(String layer, List<ResourceFilter> filters) {
		Assert.notEmpty(filters, "Filters should not be empty for custom strategy.");
		this.layer = new Layer(layer);
		this.filters.addAll(filters);
	}

	public Layer getLayer() {
		return this.layer;
	}

	@Override
	public Layer getMatchingLayer(String resourceName) {
		boolean isIncluded = false;
		for (ResourceFilter filter : this.filters) {
			if (filter.isResourceExcluded(resourceName)) {
				return null;
			}
			if (!isIncluded && filter.isResourceIncluded(resourceName)) {
				isIncluded = true;
			}
		}
		return (isIncluded) ? this.layer : null;
	}

}
