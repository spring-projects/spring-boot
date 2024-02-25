/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.File;

import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.specs.Spec;

import org.springframework.boot.gradle.tasks.bundling.ResolvedDependencies.DependencyDescriptor;
import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCoordinates;

/**
 * Resolver backed by a {@link LayeredSpec} that provides the destination {@link Layer}
 * for each copied {@link FileCopyDetails}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Paddy Drury
 * @see BootZipCopyAction
 */
class LayerResolver {

	private final ResolvedDependencies resolvedDependencies;

	private final LayeredSpec layeredConfiguration;

	private final Spec<FileCopyDetails> librarySpec;

	/**
	 * Constructs a new LayerResolver with the specified resolved dependencies, layered
	 * configuration, and library specification.
	 * @param resolvedDependencies the resolved dependencies for the layer resolver
	 * @param layeredConfiguration the layered configuration for the layer resolver
	 * @param librarySpec the library specification for the layer resolver
	 */
	LayerResolver(ResolvedDependencies resolvedDependencies, LayeredSpec layeredConfiguration,
			Spec<FileCopyDetails> librarySpec) {
		this.resolvedDependencies = resolvedDependencies;
		this.layeredConfiguration = layeredConfiguration;
		this.librarySpec = librarySpec;
	}

	/**
	 * Returns the layer for the given file copy details.
	 * @param details the file copy details
	 * @return the layer for the given file copy details, or null if an unsupported
	 * operation occurs
	 */
	Layer getLayer(FileCopyDetails details) {
		try {
			if (this.librarySpec.isSatisfiedBy(details)) {
				return getLayer(asLibrary(details));
			}
			return getLayer(details.getSourcePath());
		}
		catch (UnsupportedOperationException ex) {
			return null;
		}
	}

	/**
	 * Retrieves the layer associated with the given library from the layered
	 * configuration.
	 * @param library the library for which to retrieve the layer
	 * @return the layer associated with the given library
	 */
	Layer getLayer(Library library) {
		return this.layeredConfiguration.asLayers().getLayer(library);
	}

	/**
	 * Retrieves the layer associated with the given application resource.
	 * @param applicationResource the application resource for which to retrieve the layer
	 * @return the layer associated with the application resource
	 */
	Layer getLayer(String applicationResource) {
		return this.layeredConfiguration.asLayers().getLayer(applicationResource);
	}

	/**
	 * Returns an iterable of layers in the LayerResolver's layered configuration.
	 * @return an iterable of layers in the LayerResolver's layered configuration
	 */
	Iterable<Layer> getLayers() {
		return this.layeredConfiguration.asLayers();
	}

	/**
	 * Creates a Library object based on the given FileCopyDetails.
	 * @param details the FileCopyDetails containing the file information
	 * @return a Library object representing the file as a library
	 */
	private Library asLibrary(FileCopyDetails details) {
		File file = details.getFile();
		DependencyDescriptor dependency = this.resolvedDependencies.find(file);
		if (dependency == null) {
			return new Library(null, file, null, null, false, false, true);
		}
		LibraryCoordinates coordinates = dependency.getCoordinates();
		boolean projectDependency = dependency.isProjectDependency();
		return new Library(null, file, null, coordinates, false, projectDependency, true);
	}

}
