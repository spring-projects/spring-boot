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

package org.springframework.boot.gradle.tasks.bundling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.tasks.Input;

import org.springframework.boot.loader.tools.layer.application.FilteredResourceStrategy;
import org.springframework.boot.loader.tools.layer.application.LocationFilter;
import org.springframework.boot.loader.tools.layer.application.ResourceFilter;
import org.springframework.boot.loader.tools.layer.application.ResourceStrategy;
import org.springframework.boot.loader.tools.layer.library.CoordinateFilter;
import org.springframework.boot.loader.tools.layer.library.FilteredLibraryStrategy;
import org.springframework.boot.loader.tools.layer.library.LibraryFilter;
import org.springframework.boot.loader.tools.layer.library.LibraryStrategy;
import org.springframework.util.Assert;

/**
 * Encapsulates the configuration for a layered jar.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 2.3.0
 */
public class LayerConfiguration {

	private boolean includeLayerTools = true;

	private List<String> layerNames = new ArrayList<>();

	private List<ResourceStrategy> resourceStrategies = new ArrayList<>();

	private List<LibraryStrategy> libraryStrategies = new ArrayList<>();

	private StrategySpec strategySpec;

	/**
	 * Whether to include the layer tools jar.
	 * @return true if layer tools is included
	 */
	@Input
	public boolean isIncludeLayerTools() {
		return this.includeLayerTools;
	}

	public void setIncludeLayerTools(boolean includeLayerTools) {
		this.includeLayerTools = includeLayerTools;
	}

	@Input
	public List<String> getLayers() {
		return this.layerNames;
	}

	public void layers(String... layers) {
		this.layerNames = Arrays.asList(layers);
	}

	public void layers(List<String> layers) {
		this.layerNames = layers;
	}

	@Input
	public List<ResourceStrategy> getClasses() {
		return this.resourceStrategies;
	}

	public void classes(ResourceStrategy... resourceStrategies) {
		this.resourceStrategies = Arrays.asList(resourceStrategies);
	}

	public void classes(Action<LayerConfiguration> config) {
		this.strategySpec = StrategySpec.forResources();
		config.execute(this);
	}

	@Input
	public List<LibraryStrategy> getLibraries() {
		return this.libraryStrategies;
	}

	public void libraries(LibraryStrategy... strategies) {
		this.libraryStrategies = Arrays.asList(strategies);
	}

	public void libraries(Action<LayerConfiguration> configure) {
		this.strategySpec = StrategySpec.forLibraries();
		configure.execute(this);
	}

	public void layerContent(String layerName, Action<LayerConfiguration> config) {
		this.strategySpec.newStrategy();
		config.execute(this);
		if (this.strategySpec.isLibrariesStrategy()) {
			this.libraryStrategies.add(new FilteredLibraryStrategy(layerName, this.strategySpec.libraryFilters()));
		}
		else {
			this.resourceStrategies.add(new FilteredResourceStrategy(layerName, this.strategySpec.resourceFilters()));
		}
	}

	public void coordinates(Action<LayerConfiguration> config) {
		Assert.state(this.strategySpec.isLibrariesStrategy(),
				"The 'coordinates' filter must be used only with libraries");
		this.strategySpec.newFilter();
		config.execute(this);
		this.strategySpec
				.addLibraryFilter(new CoordinateFilter(this.strategySpec.includes(), this.strategySpec.excludes()));
	}

	public void locations(Action<LayerConfiguration> config) {
		Assert.state(this.strategySpec.isResourcesStrategy(), "The 'locations' filter must be used only with classes");
		this.strategySpec.newFilter();
		config.execute(this);
		this.strategySpec
				.addResourceFilter(new LocationFilter(this.strategySpec.includes(), this.strategySpec.excludes()));
	}

	public void include(String... includes) {
		this.strategySpec.include(includes);
	}

	public void exclude(String... excludes) {
		this.strategySpec.exclude(excludes);
	}

	private static final class StrategySpec {

		private enum TYPE {

			LIBRARIES, RESOURCES;

		}

		private final TYPE type;

		private List<LibraryFilter> libraryFilters;

		private List<ResourceFilter> resourceFilters;

		private List<String> filterIncludes;

		private List<String> filterExcludes;

		private StrategySpec(TYPE type) {
			this.type = type;
		}

		private boolean isLibrariesStrategy() {
			return this.type == TYPE.LIBRARIES;
		}

		private boolean isResourcesStrategy() {
			return this.type == TYPE.RESOURCES;
		}

		private void newStrategy() {
			this.libraryFilters = new ArrayList<>();
			this.resourceFilters = new ArrayList<>();
			newFilter();
		}

		private void newFilter() {
			this.filterIncludes = new ArrayList<>();
			this.filterExcludes = new ArrayList<>();
		}

		private List<LibraryFilter> libraryFilters() {
			return this.libraryFilters;
		}

		private void addLibraryFilter(LibraryFilter filter) {
			this.libraryFilters.add(filter);
		}

		private List<ResourceFilter> resourceFilters() {
			return this.resourceFilters;
		}

		private void addResourceFilter(ResourceFilter filter) {
			this.resourceFilters.add(filter);
		}

		private List<String> includes() {
			return this.filterIncludes;
		}

		private void include(String... includes) {
			this.filterIncludes.addAll(Arrays.asList(includes));
		}

		private void exclude(String... excludes) {
			this.filterIncludes.addAll(Arrays.asList(excludes));
		}

		private List<String> excludes() {
			return this.filterExcludes;
		}

		private static StrategySpec forLibraries() {
			return new StrategySpec(TYPE.LIBRARIES);
		}

		private static StrategySpec forResources() {
			return new StrategySpec(TYPE.RESOURCES);
		}

	}

}
