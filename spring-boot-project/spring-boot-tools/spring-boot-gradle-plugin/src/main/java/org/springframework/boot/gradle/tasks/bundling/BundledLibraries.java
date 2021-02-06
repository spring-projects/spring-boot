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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import org.springframework.boot.gradle.tasks.bundling.ResolvedDependencies.DependencyDescriptor;
import org.springframework.boot.loader.tools.BundledLibrariesWriter;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.ZipFileDetector;

/**
 * {@link Task} for generating a
 * {@value BundledLibrariesWriter#BUNDLED_LIBRARIES_FILE_NAME} file from a
 * {@code Project}.
 *
 * @author Phil Clay
 * @since 2.5.0
 */
public class BundledLibraries extends ConventionTask {

	private final RegularFileProperty outputFile;

	private final ResolvedDependencies resolvedDependencies = new ResolvedDependencies();

	private final ZipFileDetector zipFileDetector = new ZipFileDetector();

	private FileCollection classpath;

	public BundledLibraries() {
		Project project = getProject();
		this.outputFile = project.getObjects().fileProperty();

		project.getConfigurations().all((configuration) -> {
			ResolvableDependencies incoming = configuration.getIncoming();
			incoming.afterResolve((resolvableDependencies) -> {
				if (resolvableDependencies == incoming) {
					this.resolvedDependencies.processConfiguration(project, configuration);
				}
			});
		});
	}

	/**
	 * Generates the {@value BundledLibrariesWriter#BUNDLED_LIBRARIES_FILE_NAME} file in
	 * the configured {@link #getOutputFile()} outputFile}.
	 */
	@TaskAction
	public void generateBundledLibrariesYaml() {

		try {
			List<Library> libraries = getLibraries();

			File outputFile = this.outputFile.getAsFile().get();
			createFileIfNecessary(outputFile);

			try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
				new BundledLibrariesWriter().writeBundledLibraries(libraries, fileOutputStream);
			}
		}
		catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	private List<Library> getLibraries() {
		return StreamSupport.stream(this.classpath.spliterator(), false)
				.filter((file) -> file.isFile() && this.zipFileDetector.isZip(file)).map((file) -> {
					DependencyDescriptor dependency = this.resolvedDependencies.find(file);
					if (dependency == null) {
						return null;
					}
					return new Library(null, // name
							file, null, // scope
							dependency.getCoordinates(), false, // unpackRequired
							dependency.isProjectDependency());

				}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private void createFileIfNecessary(File file) throws IOException {
		if (file.exists()) {
			return;
		}
		File parent = file.getParentFile();
		if (!parent.isDirectory() && !parent.mkdirs()) {
			throw new IllegalStateException("Cannot create parent directory for '" + file.getAbsolutePath() + "'");
		}
		if (!file.createNewFile()) {
			throw new IllegalStateException("Cannot create target file '" + file.getAbsolutePath() + "'");
		}
	}

	/**
	 * Returns the property for the file to which to write the bundled libraries.
	 * @return the output file
	 */
	@OutputFile
	public RegularFileProperty getOutputFile() {
		return this.outputFile;
	}

	/**
	 * Returns the classpath that is included in the archive.
	 * @return the classpath
	 */
	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	/**
	 * Sets the classpath that is included in the archive.
	 * @param classpath the classpath
	 */
	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}

}
