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

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
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

		FileCollection custom = this.customConfigurationName != null ? this.project
				.getConfigurations().findByName(this.customConfigurationName) : null;

		if (custom != null) {
			libraries(LibraryScope.CUSTOM, custom, callback);
		}
		else {
			FileCollection compile = this.project.getConfigurations()
					.getByName("compile");

			FileCollection runtime = this.project.getConfigurations()
					.getByName("runtime");
			runtime = runtime.minus(compile);

			FileCollection provided = this.project.getConfigurations()
					.findByName(this.providedConfigurationName);

			if (provided != null) {
				compile = compile.minus(provided);
				runtime = runtime.minus(provided);
			}

			libraries(LibraryScope.COMPILE, compile, callback);
			libraries(LibraryScope.RUNTIME, runtime, callback);
			libraries(LibraryScope.PROVIDED, provided, callback);
		}
	}

	private void libraries(LibraryScope scope, FileCollection files,
			LibraryCallback callback) throws IOException {
		if (files != null) {
			for (File file: files) {
				callback.library(new Library(file, scope));
			}
		}
	}
}
