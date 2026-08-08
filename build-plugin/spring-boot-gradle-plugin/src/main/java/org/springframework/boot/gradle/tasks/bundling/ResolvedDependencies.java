/*
 * Copyright 2012-present the original author or authors.
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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.loader.tools.LibraryCoordinates;

/**
 * Maps from {@link File} to {@link ComponentArtifactIdentifier}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Paddy Drury
 * @author Andy Wilkinson
 * @author Greg Taube
 */
class ResolvedDependencies {

	private final ListProperty<ComponentArtifactIdentifier> artifactIds;

	private final ListProperty<File> artifactFiles;

	private final ListProperty<String> artifactProjectGroups;

	private final ListProperty<String> artifactProjectNames;

	private final ListProperty<String> artifactProjectVersions;

	ResolvedDependencies(Project project) {
		this.artifactIds = project.getObjects().listProperty(ComponentArtifactIdentifier.class);
		this.artifactFiles = project.getObjects().listProperty(File.class);
		this.artifactProjectGroups = project.getObjects().listProperty(String.class);
		this.artifactProjectNames = project.getObjects().listProperty(String.class);
		this.artifactProjectVersions = project.getObjects().listProperty(String.class);
	}

	@Input
	ListProperty<ComponentArtifactIdentifier> getArtifactIds() {
		return this.artifactIds;
	}

	@Classpath
	ListProperty<File> getArtifactFiles() {
		return this.artifactFiles;
	}

	@Input
	ListProperty<String> getArtifactProjectGroups() {
		return this.artifactProjectGroups;
	}

	@Input
	ListProperty<String> getArtifactProjectNames() {
		return this.artifactProjectNames;
	}

	@Input
	ListProperty<String> getArtifactProjectVersions() {
		return this.artifactProjectVersions;
	}

	void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts) {
		this.artifactFiles.addAll(
				resolvedArtifacts.map((artifacts) -> artifacts.stream().map(ResolvedArtifactResult::getFile).toList()));
		this.artifactIds.addAll(
				resolvedArtifacts.map((artifacts) -> artifacts.stream().map(ResolvedArtifactResult::getId).toList()));
		this.artifactProjectGroups.addAll(resolvedArtifacts
			.map((artifacts) -> artifacts.stream().map(ResolvedDependencies::projectGroup).toList()));
		this.artifactProjectNames.addAll(resolvedArtifacts
			.map((artifacts) -> artifacts.stream().map(ResolvedDependencies::projectName).toList()));
		this.artifactProjectVersions.addAll(resolvedArtifacts
			.map((artifacts) -> artifacts.stream().map(ResolvedDependencies::projectVersion).toList()));
	}

	private static String projectGroup(ResolvedArtifactResult artifact) {
		Capability capability = projectCapability(artifact);
		return (capability != null) ? capability.getGroup() : "";
	}

	private static String projectName(ResolvedArtifactResult artifact) {
		Capability capability = projectCapability(artifact);
		return (capability != null) ? capability.getName() : "";
	}

	private static String projectVersion(ResolvedArtifactResult artifact) {
		Capability capability = projectCapability(artifact);
		return (capability != null) ? Objects.toString(capability.getVersion(), "") : "";
	}

	private static @Nullable Capability projectCapability(ResolvedArtifactResult artifact) {
		ComponentIdentifier componentIdentifier = artifact.getId().getComponentIdentifier();
		if (!(componentIdentifier instanceof ProjectComponentIdentifier projectComponentIdentifier)) {
			return null;
		}
		List<? extends Capability> capabilities = artifact.getVariant().getCapabilities();
		for (Capability candidate : capabilities) {
			if (candidate.getName().equals(projectComponentIdentifier.getProjectName())) {
				return candidate;
			}
		}
		return capabilities.get(0);
	}

	@Nullable DependencyDescriptor find(File file) {
		int artifactIndex = findArtifactIndex(file);
		if (artifactIndex < 0) {
			return null;
		}
		ComponentArtifactIdentifier id = this.artifactIds.get().get(artifactIndex);
		if (id instanceof ModuleComponentArtifactIdentifier moduleComponentId) {
			ModuleComponentIdentifier moduleId = moduleComponentId.getComponentIdentifier();
			return new DependencyDescriptor(
					LibraryCoordinates.of(moduleId.getGroup(), moduleId.getModule(), moduleId.getVersion()), false);
		}
		ComponentIdentifier componentIdentifier = id.getComponentIdentifier();
		if (componentIdentifier instanceof ProjectComponentIdentifier) {
			LibraryCoordinates projectCoordinates = LibraryCoordinates.of(
					this.artifactProjectGroups.get().get(artifactIndex),
					this.artifactProjectNames.get().get(artifactIndex),
					this.artifactProjectVersions.get().get(artifactIndex));
			return new DependencyDescriptor(projectCoordinates, true);
		}
		return null;
	}

	private int findArtifactIndex(File file) {
		List<File> files = this.artifactFiles.get();
		for (int i = 0; i < files.size(); i++) {
			if (file.equals(files.get(i))) {
				return i;
			}
		}
		return -1;
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
