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

package org.springframework.boot.gradle.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * Expose Gradle {@link Configuration}s as {@link Libraries}.
 * 
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ProjectLibraries implements Libraries {

	private static final Set<String> SUPPORTED_TYPES = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList("jar", "ejb",
					"ejb-client", "test-jar", "bundle")));

	private final Project project;

	private String providedConfigurationName = "providedRuntime";

	private String customConfigurationName = null;

	/**
	 * Create a new {@link ProjectLibraries} instance of the specified {@link Project}.
	 * 
	 * @param project the gradle project
	 */
	public ProjectLibraries(Project project) {
		this.project = project;
	}

	/**
	 * Set the name of the provided configuration. Defaults to 'providedRuntime'.
	 * 
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

		Configuration custom = this.customConfigurationName != null ? this.project
				.getConfigurations().findByName(this.customConfigurationName) : null;

		if (custom != null) {
			libraries(LibraryScope.CUSTOM, getResolvedArtifacts(custom), callback);
		}
		else {
			Set<ResolvedArtifact> compileArtifacts = getResolvedArtifacts("compile");
			Set<ResolvedArtifact> runtimeArtifacts = getResolvedArtifacts("runtime");
			runtimeArtifacts.removeAll(compileArtifacts);

			Set<ResolvedArtifact> providedArtifacts = getResolvedArtifacts(this.providedConfigurationName);
			compileArtifacts.removeAll(providedArtifacts);
			runtimeArtifacts.removeAll(providedArtifacts);

			libraries(LibraryScope.COMPILE, compileArtifacts, callback);
			libraries(LibraryScope.RUNTIME, runtimeArtifacts, callback);
			libraries(LibraryScope.PROVIDED, providedArtifacts, callback);
		}
	}

	private Set<ResolvedArtifact> getResolvedArtifacts(Configuration configuration) {
		if (configuration == null) {
			return Collections.emptySet();
		}
		return configuration.getResolvedConfiguration().getResolvedArtifacts();
	}

	private Set<ResolvedArtifact> getResolvedArtifacts(String configurationName) {
		Configuration configuration = this.project.getConfigurations().findByName(
				configurationName);
		return getResolvedArtifacts(configuration);
	}

	private void libraries(LibraryScope scope, Set<ResolvedArtifact> artifacts,
			LibraryCallback callback) throws IOException {
		for (ResolvedArtifact artifact : artifacts) {
			if (SUPPORTED_TYPES.contains(artifact.getType())) {
				callback.library(artifact.getFile(), scope);
			}
		}
	}
}
