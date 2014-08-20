/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.repackage;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * Expose Gradle {@link Configuration}s as {@link Libraries}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ProjectLibraries implements Libraries {

	private final Project project;

	private final SpringBootPluginExtension extension;

	private String providedConfigurationName = "providedRuntime";

	private String customConfigurationName = null;

	/**
	 * Create a new {@link ProjectLibraries} instance of the specified {@link Project}.
	 * @param project the gradle project
	 * @param extension the extension
	 */
	public ProjectLibraries(Project project, SpringBootPluginExtension extension) {
		this.project = project;
		this.extension = extension;
	}

	/**
	 * Set the name of the provided configuration. Defaults to 'providedRuntime'.
	 * @param providedConfigurationName the providedConfigurationName to set
	 */
	public void setProvidedConfigurationName(String providedConfigurationName) {
		this.providedConfigurationName = providedConfigurationName;
	}

	public void setCustomConfigurationName(String customConfigurationName) {
		this.customConfigurationName = customConfigurationName;
	}

	@Override
	public void doWithLibraries(LibraryCallback callback) throws IOException {
		Set<Library> custom = getLibraries(this.customConfigurationName,
				LibraryScope.CUSTOM);
		if (custom != null) {
			libraries(custom, callback);
		}
		else {
			Set<Library> compile = getLibraries("compile", LibraryScope.COMPILE);

			Set<Library> runtime = getLibraries("runtime", LibraryScope.RUNTIME);
			runtime = minus(runtime, compile);

			Set<Library> provided = getLibraries(this.providedConfigurationName,
					LibraryScope.PROVIDED);
			if (provided != null) {
				compile = minus(compile, provided);
				runtime = minus(runtime, provided);
			}

			libraries(compile, callback);
			libraries(runtime, callback);
			libraries(provided, callback);
		}
	}

	private Set<Library> getLibraries(String configurationName, LibraryScope scope) {
		Configuration configuration = (configurationName == null ? null : this.project
				.getConfigurations().findByName(configurationName));
		if (configuration == null) {
			return null;
		}
		Set<Library> libraries = new LinkedHashSet<Library>();
		for (ResolvedArtifact artifact : configuration.getResolvedConfiguration()
				.getResolvedArtifacts()) {
			libraries.add(new ResolvedArtifactLibrary(artifact, scope));
		}
		libraries.addAll(getLibrariesForFileDependencies(configuration, scope));

		return libraries;
	}

	private Set<Library> getLibrariesForFileDependencies(Configuration configuration,
			LibraryScope scope) {
		Set<Library> libraries = new LinkedHashSet<Library>();
		for (Dependency dependency : configuration.getIncoming().getDependencies()) {
			if (dependency instanceof FileCollectionDependency) {
				FileCollectionDependency fileDependency = (FileCollectionDependency) dependency;
				for (File file : fileDependency.resolve()) {
					libraries.add(new Library(file, scope));
				}
			}
			else if (dependency instanceof ProjectDependency) {
				ProjectDependency projectDependency = (ProjectDependency) dependency;
				libraries.addAll(getLibrariesForFileDependencies(
						projectDependency.getProjectConfiguration(), scope));
			}
		}
		return libraries;
	}

	private Set<Library> minus(Set<Library> source, Set<Library> toRemove) {
		if (source == null || toRemove == null) {
			return source;
		}
		Set<File> filesToRemove = new HashSet<File>();
		for (Library library : toRemove) {
			filesToRemove.add(library.getFile());
		}
		Set<Library> result = new LinkedHashSet<Library>();
		for (Library library : source) {
			if (!filesToRemove.contains(library.getFile())) {
				result.add(library);
			}
		}
		return result;
	}

	private void libraries(Set<Library> libraries, LibraryCallback callback)
			throws IOException {
		if (libraries != null) {
			for (Library library : libraries) {
				callback.library(library);
			}
		}
	}

	/**
	 * Adapts a {@link ResolvedArtifact} to a {@link Library}.
	 */
	private class ResolvedArtifactLibrary extends Library {

		private final ResolvedArtifact artifact;

		public ResolvedArtifactLibrary(ResolvedArtifact artifact, LibraryScope scope) {
			super(artifact.getFile(), scope);
			this.artifact = artifact;
		}

		@Override
		public boolean isUnpackRequired() {
			if (ProjectLibraries.this.extension.getRequiresUnpack() != null) {
				ModuleVersionIdentifier id = this.artifact.getModuleVersion().getId();
				return ProjectLibraries.this.extension.getRequiresUnpack().contains(
						id.getGroup() + ":" + id.getName());
			}
			return false;
		}

	}

}
