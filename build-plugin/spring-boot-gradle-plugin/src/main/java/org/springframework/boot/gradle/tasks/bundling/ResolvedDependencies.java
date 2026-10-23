/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.loader.tools.LibraryCoordinates;

/**
 * Maps archive classpath files to their dependency coordinates.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Paddy Drury
 * @author Andy Wilkinson
 * @author Greg Taube
 */
class ResolvedDependencies {

	private final ListProperty<ResolvedArtifact> artifacts;

	ResolvedDependencies(Project project) {
		this.artifacts = project.getObjects().listProperty(ResolvedArtifact.class);
		this.artifacts.finalizeValueOnRead();
	}

	@Nested
	ListProperty<ResolvedArtifact> getArtifacts() {
		return this.artifacts;
	}

	void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts, Project project) {
		Map<String, LibraryCoordinates> coordinates = project.getRootProject()
			.getAllprojects()
			.stream()
			.collect(Collectors.toMap(Project::getPath,
					(candidate) -> LibraryCoordinates.of(Objects.toString(candidate.getGroup()), candidate.getName(),
							Objects.toString(candidate.getVersion()))));
		this.artifacts.addAll(resolvedArtifacts.map((artifacts) -> artifacts.stream().map((artifact) -> {
			ComponentIdentifier id = artifact.getId().getComponentIdentifier();
			LibraryCoordinates projectCoordinates = (id instanceof ProjectComponentIdentifier projectId)
					? coordinates.get(projectId.getProjectPath()) : null;
			return new ResolvedArtifact(artifact, projectCoordinates);
		}).toList()));
	}

	void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts,
			Provider<ResolvedComponentResult> rootComponent) {
		this.artifacts.addAll(resolvedArtifacts.zip(rootComponent.map(ResolvedDependencies::projectCoordinates),
				(artifacts, coordinates) -> artifacts.stream().map((artifact) -> {
					ComponentIdentifier id = artifact.getId().getComponentIdentifier();
					LibraryCoordinates projectCoordinates = coordinates.get(id);
					if (id instanceof ProjectComponentIdentifier && projectCoordinates == null) {
						throw new GradleException("No project coordinates found for " + id
								+ "; resolved artifacts and root component must come from the same configuration");
					}
					return new ResolvedArtifact(artifact, projectCoordinates);
				}).toList()));
	}

	private static Map<ComponentIdentifier, LibraryCoordinates> projectCoordinates(ResolvedComponentResult root) {
		Map<ComponentIdentifier, LibraryCoordinates> coordinates = new HashMap<>();
		Set<ComponentIdentifier> visited = new HashSet<>();
		ArrayDeque<ResolvedComponentResult> components = new ArrayDeque<>();
		components.add(root);
		while (!components.isEmpty()) {
			ResolvedComponentResult component = components.removeFirst();
			ComponentIdentifier id = component.getId();
			if (!visited.add(id)) {
				continue;
			}
			if (id instanceof ProjectComponentIdentifier) {
				ModuleVersionIdentifier version = component.getModuleVersion();
				if (version != null) {
					coordinates.put(id,
							LibraryCoordinates.of(version.getGroup(), version.getName(), version.getVersion()));
				}
			}
			component.getDependencies().forEach((dependency) -> {
				if (dependency instanceof ResolvedDependencyResult resolved) {
					components.add(resolved.getSelected());
				}
			});
		}
		return coordinates;
	}

	@Nullable DependencyDescriptor find(File file) {
		for (ResolvedArtifact artifact : this.artifacts.get()) {
			if (file.equals(artifact.getFile())) {
				return (artifact.getArtifactId() != null) ? new DependencyDescriptor(artifact,
						artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) : null;
			}
		}
		return null;
	}

	/**
	 * Task inputs for one artifact. Resolution results are only used while obtaining
	 * these inputs and are not retained for task execution or configuration-cache reuse.
	 */
	static final class ResolvedArtifact implements LibraryCoordinates {

		private final File file;

		private final ComponentArtifactIdentifier id;

		private final @Nullable String groupId;

		private final @Nullable String artifactId;

		private final @Nullable String version;

		private ResolvedArtifact(ResolvedArtifactResult artifact, @Nullable LibraryCoordinates coordinates) {
			this.file = artifact.getFile();
			this.id = artifact.getId();
			if (this.id.getComponentIdentifier() instanceof ModuleComponentIdentifier module) {
				coordinates = LibraryCoordinates.of(module.getGroup(), module.getModule(), module.getVersion());
			}
			this.groupId = (coordinates != null) ? coordinates.getGroupId() : null;
			this.artifactId = (coordinates != null) ? coordinates.getArtifactId() : null;
			this.version = (coordinates != null) ? coordinates.getVersion() : null;
		}

		@Classpath
		File getFile() {
			return this.file;
		}

		@Input
		ComponentArtifactIdentifier getId() {
			return this.id;
		}

		@Override
		@Input
		@Optional
		public @Nullable String getGroupId() {
			return this.groupId;
		}

		@Override
		@Input
		@Optional
		public @Nullable String getArtifactId() {
			return this.artifactId;
		}

		@Override
		@Input
		@Optional
		public @Nullable String getVersion() {
			return this.version;
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
