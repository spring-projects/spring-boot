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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.util.ConfigureUtil;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Layers;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.layer.ApplicationContentFilter;
import org.springframework.boot.loader.tools.layer.ContentFilter;
import org.springframework.boot.loader.tools.layer.ContentSelector;
import org.springframework.boot.loader.tools.layer.CustomLayers;
import org.springframework.boot.loader.tools.layer.IncludeExcludeContentSelector;
import org.springframework.boot.loader.tools.layer.LibraryContentFilter;
import org.springframework.util.Assert;

/**
 * Encapsulates the configuration for a layered jar.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 2.3.0
 */
public class LayeredSpec {

	private boolean includeLayerTools = true;

	private ApplicationSpec application = new ApplicationSpec();

	private DependenciesSpec dependencies = new DependenciesSpec();

	@Optional
	private List<String> layerOrder;

	private Layers layers;

	@Input
	public boolean isIncludeLayerTools() {
		return this.includeLayerTools;
	}

	public void setIncludeLayerTools(boolean includeLayerTools) {
		this.includeLayerTools = includeLayerTools;
	}

	@Input
	public ApplicationSpec getApplication() {
		return this.application;
	}

	public void application(ApplicationSpec spec) {
		this.application = spec;
	}

	public void application(Closure<?> closure) {
		application(ConfigureUtil.configureUsing(closure));
	}

	public void application(Action<ApplicationSpec> action) {
		action.execute(this.application);
	}

	@Input
	public DependenciesSpec getDependencies() {
		return this.dependencies;
	}

	public void dependencies(DependenciesSpec spec) {
		this.dependencies = spec;
	}

	public void dependencies(Closure<?> closure) {
		dependencies(ConfigureUtil.configureUsing(closure));
	}

	public void dependencies(Action<DependenciesSpec> action) {
		action.execute(this.dependencies);
	}

	@Input
	public List<String> getLayerOrder() {
		return this.layerOrder;
	}

	public void layerOrder(String... layerOrder) {
		this.layerOrder = Arrays.asList(layerOrder);
	}

	public void layerOrder(List<String> layerOrder) {
		this.layerOrder = layerOrder;
	}

	/**
	 * Return this configuration as a {@link Layers} instance. This method should only be
	 * called when the configuration is complete and will no longer be changed.
	 * @return the layers
	 */
	Layers asLayers() {
		Layers layers = this.layers;
		if (layers == null) {
			layers = createLayers();
			this.layers = layers;
		}
		return layers;
	}

	private Layers createLayers() {
		if (this.layerOrder == null || this.layerOrder.isEmpty()) {
			Assert.state(this.application.isEmpty() && this.dependencies.isEmpty(),
					"The 'layerOrder' must be defined when using custom layering");
			return Layers.IMPLICIT;
		}
		List<Layer> layers = this.layerOrder.stream().map(Layer::new).collect(Collectors.toList());
		return new CustomLayers(layers, this.application.asSelectors(), this.dependencies.asSelectors());
	}

	public abstract static class IntoLayersSpec implements Serializable {

		private final List<IntoLayerSpec> intoLayers;

		boolean isEmpty() {
			return this.intoLayers.isEmpty();
		}

		IntoLayersSpec(IntoLayerSpec... spec) {
			this.intoLayers = new ArrayList<>(Arrays.asList(spec));
		}

		public void intoLayer(String layer) {
			this.intoLayers.add(new IntoLayerSpec(layer));
		}

		public void intoLayer(String layer, Closure<?> closure) {
			intoLayer(layer, ConfigureUtil.configureUsing(closure));
		}

		public void intoLayer(String layer, Action<IntoLayerSpec> action) {
			IntoLayerSpec spec = new IntoLayerSpec(layer);
			action.execute(spec);
			this.intoLayers.add(spec);
		}

		<T> List<ContentSelector<T>> asSelectors(Function<String, ContentFilter<T>> filterFactory) {
			return this.intoLayers.stream().map((content) -> content.asSelector(filterFactory))
					.collect(Collectors.toList());
		}

	}

	public static class IntoLayerSpec implements Serializable {

		private final String intoLayer;

		private final List<String> includes = new ArrayList<>();

		private final List<String> excludes = new ArrayList<>();

		public IntoLayerSpec(String intoLayer) {
			this.intoLayer = intoLayer;
		}

		public void include(String... patterns) {
			this.includes.addAll(Arrays.asList(patterns));
		}

		public void exclude(String... patterns) {
			this.includes.addAll(Arrays.asList(patterns));
		}

		<T> ContentSelector<T> asSelector(Function<String, ContentFilter<T>> filterFactory) {
			Layer layer = new Layer(this.intoLayer);
			return new IncludeExcludeContentSelector<>(layer, this.includes, this.excludes, filterFactory);
		}

	}

	public static class ApplicationSpec extends IntoLayersSpec {

		public ApplicationSpec(IntoLayerSpec... contents) {
			super(contents);
		}

		List<ContentSelector<String>> asSelectors() {
			return asSelectors(ApplicationContentFilter::new);
		}

	}

	public static class DependenciesSpec extends IntoLayersSpec {

		public DependenciesSpec(IntoLayerSpec... contents) {
			super(contents);
		}

		List<ContentSelector<Library>> asSelectors() {
			return asSelectors(LibraryContentFilter::new);
		}

	}

}
