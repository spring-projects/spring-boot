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
import org.gradle.api.artifacts.ModuleVersionIdentifier;
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
		Set<ResolvedArtifact> custom = getArtifacts(this.customConfigurationName);
		if (custom != null) {
			libraries(LibraryScope.CUSTOM, custom, callback);
		}
		else {
			Set<ResolvedArtifact> compile = getArtifacts("compile");

			Set<ResolvedArtifact> runtime = getArtifacts("runtime");
			runtime = minus(runtime, compile);

			Set<ResolvedArtifact> provided = getArtifacts(this.providedConfigurationName);
			if (provided != null) {
				compile = minus(compile, provided);
				runtime = minus(runtime, provided);
			}

			libraries(LibraryScope.COMPILE, compile, callback);
			libraries(LibraryScope.RUNTIME, runtime, callback);
			libraries(LibraryScope.PROVIDED, provided, callback);
		}
	}

	private Set<ResolvedArtifact> getArtifacts(String configurationName) {
		Configuration configuration = (configurationName == null ? null : this.project
				.getConfigurations().findByName(configurationName));
		return (configuration == null ? null : configuration.getResolvedConfiguration()
				.getResolvedArtifacts());
	}

	private Set<ResolvedArtifact> minus(Set<ResolvedArtifact> source,
			Set<ResolvedArtifact> toRemove) {
		if (source == null || toRemove == null) {
			return source;
		}
		Set<File> filesToRemove = new HashSet<File>();
		for (ResolvedArtifact artifact : toRemove) {
			filesToRemove.add(artifact.getFile());
		}
		Set<ResolvedArtifact> result = new LinkedHashSet<ResolvedArtifact>();
		for (ResolvedArtifact artifact : source) {
			if (!filesToRemove.contains(artifact.getFile())) {
				result.add(artifact);
			}
		}
		return result;
	}

	private void libraries(LibraryScope scope, Set<ResolvedArtifact> artifacts,
			LibraryCallback callback) throws IOException {
		if (artifacts != null) {
			for (ResolvedArtifact artifact : artifacts) {
				callback.library(new Library(artifact.getFile(), scope,
						isUnpackRequired(artifact)));
			}
		}
	}

	private boolean isUnpackRequired(ResolvedArtifact artifact) {
		if (this.extension.getRequiresUnpack() != null) {
			ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
			return this.extension.getRequiresUnpack().contains(
					id.getGroup() + ":" + id.getName());
		}
		return false;
	}

}
