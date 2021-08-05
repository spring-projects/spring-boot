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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;

import org.springframework.boot.loader.tools.LibraryCoordinates;

/**
 * Tracks and provides details of resolved dependencies in the project so we can find
 * {@link LibraryCoordinates}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Paddy Drury
 * @author Andy Wilkinson
 */
class ResolvedDependencies {

	private final Map<Configuration, ResolvedConfigurationDependencies> configurationDependencies = new LinkedHashMap<>();

	private String projectId(Project project) {
		return project.getGroup() + ":" + project.getName() + ":" + project.getVersion();
	}

	void processConfiguration(Project project, Configuration configuration) {
		Set<String> localProjectIds = project.getRootProject().getAllprojects().stream().map(this::projectId)
				.collect(Collectors.toSet());
		this.configurationDependencies.put(configuration,
				new ResolvedConfigurationDependencies(localProjectIds, configuration.getResolvedConfiguration()));
	}

	DependencyDescriptor find(File file) {
		for (ResolvedConfigurationDependencies dependencies : this.configurationDependencies.values()) {
			DependencyDescriptor dependency = dependencies.find(file);
			if (dependency != null) {
				return dependency;
			}
		}
		return null;
	}

	/**
	 * Stores details of resolved configuration dependencies.
	 */
	private static class ResolvedConfigurationDependencies {

		private final Map<File, DependencyDescriptor> dependencies = new LinkedHashMap<>();

		ResolvedConfigurationDependencies(Set<String> projectDependencyIds,
				ResolvedConfiguration resolvedConfiguration) {
			if (!resolvedConfiguration.hasError()) {
				for (ResolvedArtifact resolvedArtifact : resolvedConfiguration.getResolvedArtifacts()) {
					ModuleVersionIdentifier id = resolvedArtifact.getModuleVersion().getId();
					boolean projectDependency = projectDependencyIds
							.contains(id.getGroup() + ":" + id.getName() + ":" + id.getVersion());
					this.dependencies.put(resolvedArtifact.getFile(), new DependencyDescriptor(
							new ModuleVersionIdentifierLibraryCoordinates(id), projectDependency));
				}
			}
		}

		DependencyDescriptor find(File file) {
			return this.dependencies.get(file);
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

	/**
	 * Describes a dependency in a {@link ResolvedConfiguration}.
	 */
	static final class DependencyDescriptor {

		private final LibraryCoordinates coordinates;

		private final boolean projectDependency;

		private DependencyDescriptor(LibraryCoordinates coordinates, boolean projectDependency) {
			this.coordinates = coordinates;
			this.projectDependency = projectDependency;
		}

		LibraryCoordinates getCoordinates() {
			return this.coordinates;
		}

		boolean isProjectDependency() {
			return this.projectDependency;
		}

	}

}
