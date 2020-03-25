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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.specs.Spec;

import org.springframework.boot.loader.tools.JarModeLibrary;
import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Layers;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCoordinates;

/**
 * Resolver backed by a {@link LayeredSpec} that provides the destination {@link Layer}
 * for each copied {@link FileCopyDetails}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @see BootZipCopyAction
 */
class LayerResolver {

	private static final String BOOT_INF_FOLDER = "BOOT-INF/";

	private final ResolvedDependencies resolvedDependencies;

	private final LayeredSpec layeredConfiguration;

	private final Spec<FileCopyDetails> librarySpec;

	LayerResolver(Iterable<Configuration> configurations, LayeredSpec layeredConfiguration,
			Spec<FileCopyDetails> librarySpec) {
		this.resolvedDependencies = new ResolvedDependencies(configurations);
		this.layeredConfiguration = layeredConfiguration;
		this.librarySpec = librarySpec;
	}

	String getPath(JarModeLibrary jarModeLibrary) {
		Layers layers = this.layeredConfiguration.asLayers();
		Layer layer = layers.getLayer(jarModeLibrary);
		if (layer != null) {
			return BOOT_INF_FOLDER + "layers/" + layer + "/lib/" + jarModeLibrary.getName();
		}
		return BOOT_INF_FOLDER + "lib/" + jarModeLibrary.getName();
	}

	String getPath(FileCopyDetails details) {
		String path = details.getRelativePath().getPathString();
		Layer layer = getLayer(details);
		if (layer == null || !path.startsWith(BOOT_INF_FOLDER)) {
			return path;
		}
		path = path.substring(BOOT_INF_FOLDER.length());
		return BOOT_INF_FOLDER + "layers/" + layer + "/" + path;
	}

	Layer getLayer(FileCopyDetails details) {
		Layers layers = this.layeredConfiguration.asLayers();
		try {
			if (this.librarySpec.isSatisfiedBy(details)) {
				return layers.getLayer(asLibrary(details));
			}
			return layers.getLayer(details.getSourcePath());
		}
		catch (UnsupportedOperationException ex) {
			return null;
		}
	}

	List<String> getLayerNames() {
		return this.layeredConfiguration.asLayers().stream().map(Layer::toString).collect(Collectors.toList());
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

		private final Map<Configuration, ResolvedConfigurationDependencies> configurationDependencies = new LinkedHashMap<>();

		ResolvedDependencies(Iterable<Configuration> configurations) {
			configurations.forEach(this::processConfiguration);
		}

		private void processConfiguration(Configuration configuration) {
			if (configuration.isCanBeResolved()) {
				this.configurationDependencies.put(configuration,
						new ResolvedConfigurationDependencies(configuration.getIncoming().getArtifacts()));
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

		ResolvedConfigurationDependencies(ArtifactCollection resolvedDependencies) {
			if (resolvedDependencies != null) {
				for (ResolvedArtifactResult resolvedArtifact : resolvedDependencies.getArtifacts()) {
					ComponentIdentifier identifier = resolvedArtifact.getId().getComponentIdentifier();
					if (identifier instanceof ModuleComponentIdentifier) {
						this.artifactCoordinates.put(resolvedArtifact.getFile(),
								new ModuleComponentIdentifierLibraryCoordinates(
										(ModuleComponentIdentifier) identifier));
					}
				}
			}
		}

		LibraryCoordinates find(File file) {
			return this.artifactCoordinates.get(file);
		}

	}

	/**
	 * Adapts a {@link ModuleComponentIdentifier} to {@link LibraryCoordinates}.
	 */
	private static class ModuleComponentIdentifierLibraryCoordinates implements LibraryCoordinates {

		private final ModuleComponentIdentifier identifier;

		ModuleComponentIdentifierLibraryCoordinates(ModuleComponentIdentifier identifier) {
			this.identifier = identifier;
		}

		@Override
		public String getGroupId() {
			return this.identifier.getGroup();
		}

		@Override
		public String getArtifactId() {
			return this.identifier.getModule();
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
