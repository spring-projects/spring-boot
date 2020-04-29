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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
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

	LayerResolver(Iterable<Configuration> configurations, LayeredSpec layeredConfiguration,
			Spec<FileCopyDetails> librarySpec) {
		this.resolvedDependencies = new ResolvedDependencies(configurations);
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

	/**
	 * Tracks and provides details of resolved dependencies in the project so we can find
	 * {@link LibraryCoordinates}.
	 */
	private static class ResolvedDependencies {

		private static final Set<String> DEPRECATED_FOR_RESOLUTION_CONFIGURATIONS = Collections
				.unmodifiableSet(new HashSet<>(Arrays.asList("archives", "compile", "compileOnly", "default", "runtime",
						"testCompile", "testCompileOnly", "testRuntime")));

		private final Map<Configuration, ResolvedConfigurationDependencies> configurationDependencies = new LinkedHashMap<>();

		ResolvedDependencies(Iterable<Configuration> configurations) {
			configurations.forEach(this::processConfiguration);
		}

		private void processConfiguration(Configuration configuration) {
			if (configuration.isCanBeResolved()
					&& !DEPRECATED_FOR_RESOLUTION_CONFIGURATIONS.contains(configuration.getName())) {
				this.configurationDependencies.put(configuration,
						new ResolvedConfigurationDependencies(configuration.getResolvedConfiguration()));
			}
		}

		LibraryCoordinates find(File file) {
			for (ResolvedConfigurationDependencies dependencies : this.configurationDependencies.values()) {
				LibraryCoordinates coordinates = dependencies.find(file);
				if (coordinates != null) {
					return coordinates;
				}
			}
			return null;
		}

	}

	/**
	 * Stores details of resolved configuration dependencies.
	 */
	private static class ResolvedConfigurationDependencies {

		private final Map<File, LibraryCoordinates> artifactCoordinates = new LinkedHashMap<>();

		ResolvedConfigurationDependencies(ResolvedConfiguration resolvedConfiguration) {
			for (ResolvedArtifact resolvedArtifact : resolvedConfiguration.getResolvedArtifacts()) {
				this.artifactCoordinates.put(resolvedArtifact.getFile(),
						new ModuleVersionIdentifierLibraryCoordinates(resolvedArtifact.getModuleVersion().getId()));
			}
		}

		LibraryCoordinates find(File file) {
			return this.artifactCoordinates.get(file);
		}

	}

	/**
	 * Adapts a {@link ModuleVersionIdentifier} to {@link LibraryCoordinates}.
	 */
	private static class ModuleVersionIdentifierLibraryCoordinates implements LibraryCoordinates {

		private final ModuleVersionIdentifier identifier;

		ModuleVersionIdentifierLibraryCoordinates(ModuleVersionIdentifier identifier) {
			this.identifier = identifier;
		}

		@Override
		public String getGroupId() {
			return this.identifier.getGroup();
		}

		@Override
		public String getArtifactId() {
			return this.identifier.getName();
		}

		@Override
		public String getVersion() {
			return this.identifier.getVersion();
		}

		@Override
		public String toString() {
			return this.identifier.toString();
		}

	}

}
