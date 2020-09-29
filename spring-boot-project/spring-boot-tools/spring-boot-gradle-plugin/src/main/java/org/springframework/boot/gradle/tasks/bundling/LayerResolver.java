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

import java.io.File;

import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.specs.Spec;

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

	LayerResolver(ResolvedDependencies resolvedDependencies, LayeredSpec layeredConfiguration,
			Spec<FileCopyDetails> librarySpec) {
		this.resolvedDependencies = resolvedDependencies;
		this.layeredConfiguration = layeredConfiguration;
		this.librarySpec = librarySpec;
	}

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

	Layer getLayer(Library library) {
		return this.layeredConfiguration.asLayers().getLayer(library);
	}

	Layer getLayer(String applicationResource) {
		return this.layeredConfiguration.asLayers().getLayer(applicationResource);
	}

	Iterable<Layer> getLayers() {
		return this.layeredConfiguration.asLayers();
	}

	private Library asLibrary(FileCopyDetails details) {
		File file = details.getFile();
		LibraryCoordinates coordinates = this.resolvedDependencies.find(file);
		return new Library(null, file, null, coordinates, false);
	}

}
