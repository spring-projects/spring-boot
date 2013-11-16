
package org.springframework.boot.gradle.task;

import java.io.File;
import java.io.IOException;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * Expose Gradle {@link Configuration}s as {@link Libraries}.
 * 
 * @author Phillip Webb
 */
class ProjectLibraries implements Libraries {

	private final Project project;

	private String providedConfigurationName = "providedRuntime";

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

	@Override
	public void doWithLibraries(LibraryCallback callback) throws IOException {

		FileCollection compile = this.project.getConfigurations().getByName("compile");

		FileCollection runtime = this.project.getConfigurations().getByName("runtime");
		runtime = runtime.minus(compile);

		FileCollection provided = this.project.getConfigurations().findByName(
				this.providedConfigurationName);
		if (provided != null) {
			compile = compile.minus(provided);
			runtime = runtime.minus(provided);
		}

		libraries(LibraryScope.COMPILE, compile, callback);
		libraries(LibraryScope.RUNTIME, runtime, callback);
		libraries(LibraryScope.PROVIDED, provided, callback);
	}

	private void libraries(LibraryScope scope, FileCollection files,
			LibraryCallback callback) throws IOException {
		if (files != null) {
			for (File file : files) {
				callback.library(file, scope);
			}
		}
	}
}
