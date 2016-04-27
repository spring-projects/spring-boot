/*
 * Copyright 2012-2016 the original author or authors.
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

	private final boolean excludeDevtools;

	private String providedConfigurationName = "providedRuntime";

	private String customConfigurationName = null;

	/**
	 * Create a new {@link ProjectLibraries} instance of the specified {@link Project}.
	 * @param project the gradle project
	 * @param extension the extension
	 * @param excludeDevTools whether Spring Boot Devtools should be excluded
	 */
	ProjectLibraries(Project project, SpringBootPluginExtension extension,
			boolean excludeDevTools) {
		this.project = project;
		this.extension = extension;
		this.excludeDevtools = excludeDevTools;
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
		Set<GradleLibrary> custom = getLibraries(this.customConfigurationName,
				LibraryScope.CUSTOM);
		if (custom != null) {
			libraries(custom, callback);
		}
		else {
			Set<GradleLibrary> runtime = getLibraries("runtime", LibraryScope.RUNTIME);
			Set<GradleLibrary> provided = getLibraries(this.providedConfigurationName,
					LibraryScope.PROVIDED);
			if (provided != null) {
				runtime = minus(runtime, provided);
			}
			libraries(runtime, callback);
			libraries(provided, callback);
		}
	}

	private Set<GradleLibrary> getLibraries(String configurationName,
			LibraryScope scope) {
		Configuration configuration = (configurationName == null ? null
				: this.project.getConfigurations().findByName(configurationName));
		if (configuration == null) {
			return null;
		}
		Set<GradleLibrary> libraries = new LinkedHashSet<GradleLibrary>();
		for (ResolvedArtifact artifact : configuration.getResolvedConfiguration()
				.getResolvedArtifacts()) {
			libraries.add(new ResolvedArtifactLibrary(artifact, scope));
		}
		libraries.addAll(getLibrariesForFileDependencies(configuration, scope));
		return libraries;
	}

	private Set<GradleLibrary> getLibrariesForFileDependencies(
			Configuration configuration, LibraryScope scope) {
		Set<GradleLibrary> libraries = new LinkedHashSet<GradleLibrary>();
		for (Dependency dependency : configuration.getIncoming().getDependencies()) {
			if (dependency instanceof FileCollectionDependency) {
				FileCollectionDependency fileDependency = (FileCollectionDependency) dependency;
				for (File file : fileDependency.resolve()) {
					libraries.add(
							new GradleLibrary(fileDependency.getGroup(), file, scope));
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

	private Set<GradleLibrary> minus(Set<GradleLibrary> source,
			Set<GradleLibrary> toRemove) {
		if (source == null || toRemove == null) {
			return source;
		}
		Set<File> filesToRemove = new HashSet<File>();
		for (GradleLibrary library : toRemove) {
			filesToRemove.add(library.getFile());
		}
		Set<GradleLibrary> result = new LinkedHashSet<GradleLibrary>();
		for (GradleLibrary library : source) {
			if (!filesToRemove.contains(library.getFile())) {
				result.add(library);
			}
		}
		return result;
	}

	private void libraries(Set<GradleLibrary> libraries, LibraryCallback callback)
			throws IOException {
		if (libraries != null) {
			Set<String> duplicates = getDuplicates(libraries);
			for (GradleLibrary library : libraries) {
				if (!isExcluded(library)) {
					library.setIncludeGroupName(duplicates.contains(library.getName()));
					callback.library(library);
				}
			}
		}
	}

	private boolean isExcluded(GradleLibrary library) {
		if (this.excludeDevtools && isDevToolsJar(library)) {
			return true;
		}
		return false;
	}

	private boolean isDevToolsJar(GradleLibrary library) {
		return "org.springframework.boot".equals(library.getGroup())
				&& library.getName().startsWith("spring-boot-devtools");
	}

	private Set<String> getDuplicates(Set<GradleLibrary> libraries) {
		Set<String> duplicates = new HashSet<String>();
		Set<String> seen = new HashSet<String>();
		for (GradleLibrary library : libraries) {
			if (library.getFile() != null && !seen.add(library.getFile().getName())) {
				duplicates.add(library.getFile().getName());
			}
		}
		return duplicates;
	}

	private class GradleLibrary extends Library {

		private final String group;

		private boolean includeGroupName;

		GradleLibrary(String group, File file, LibraryScope scope) {
			super(file, scope);
			this.group = group;
		}

		public void setIncludeGroupName(boolean includeGroupName) {
			this.includeGroupName = includeGroupName;
		}

		public String getGroup() {
			return this.group;
		}

		@Override
		public String getName() {
			String name = super.getName();
			if (this.includeGroupName && this.group != null) {
				name = this.group + "-" + name;
			}
			return name;
		}

		@Override
		public int hashCode() {
			return getFile().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof GradleLibrary) {
				return getFile().equals(((GradleLibrary) obj).getFile());
			}
			return false;
		}

		@Override
		public String toString() {
			return getFile().getAbsolutePath();
		}
	}

	/**
	 * Adapts a {@link ResolvedArtifact} to a {@link Library}.
	 */
	private class ResolvedArtifactLibrary extends GradleLibrary {

		private final ResolvedArtifact artifact;

		ResolvedArtifactLibrary(ResolvedArtifact artifact, LibraryScope scope) {
			super(artifact.getModuleVersion().getId().getGroup(), artifact.getFile(),
					scope);
			this.artifact = artifact;
		}

		@Override
		public boolean isUnpackRequired() {
			if (ProjectLibraries.this.extension.getRequiresUnpack() != null) {
				ModuleVersionIdentifier id = this.artifact.getModuleVersion().getId();
				return ProjectLibraries.this.extension.getRequiresUnpack()
						.contains(id.getGroup() + ":" + id.getName());
			}
			return false;
		}

	}

}
