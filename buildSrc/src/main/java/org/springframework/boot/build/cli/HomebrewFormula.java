/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.build.cli;

import java.io.File;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import org.springframework.boot.build.artifacts.ArtifactRelease;

/**
 * A {@link Task} for creating a Homebrew formula manifest.
 *
 * @author Andy Wilkinson
 */
public class HomebrewFormula extends DefaultTask {

	private Provider<RegularFile> archive;

	private File template;

	private File outputDir;

	/**
	 * Constructor for HomebrewFormula class.
	 *
	 * This method initializes the HomebrewFormula object and sets the version property to
	 * the current version of the project.
	 * @param version the version of the project
	 */
	public HomebrewFormula() {
		getInputs().property("version", getProject().provider(getProject()::getVersion));
	}

	/**
	 * Retrieves the archive file associated with this HomebrewFormula.
	 * @return The archive file.
	 */
	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFile getArchive() {
		return this.archive.get();
	}

	/**
	 * Sets the archive for the HomebrewFormula.
	 * @param archive the Provider object representing the archive file
	 */
	public void setArchive(Provider<RegularFile> archive) {
		this.archive = archive;
	}

	/**
	 * Returns the template file used by this HomebrewFormula.
	 * @return the template file
	 */
	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public File getTemplate() {
		return this.template;
	}

	/**
	 * Sets the template file for the HomebrewFormula.
	 * @param template the template file to be set
	 */
	public void setTemplate(File template) {
		this.template = template;
	}

	/**
	 * Returns the output directory for the HomebrewFormula.
	 * @return the output directory
	 */
	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	/**
	 * Sets the output directory for the HomebrewFormula.
	 * @param outputDir the output directory to be set
	 */
	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	/**
	 * Creates a descriptor using the provided additional properties.
	 * @param additionalProperties a map of additional properties to be used in the
	 * descriptor creation
	 */
	protected void createDescriptor(Map<String, Object> additionalProperties) {
		getProject().copy((copy) -> {
			copy.from(this.template);
			copy.into(this.outputDir);
			copy.expand(getProperties(additionalProperties));
		});
	}

	/**
	 * Returns a map of properties for the HomebrewFormula.
	 * @param additionalProperties a map of additional properties to be included in the
	 * result
	 * @return a map of properties including the additional properties, hash, repo, and
	 * project
	 */
	private Map<String, Object> getProperties(Map<String, Object> additionalProperties) {
		Map<String, Object> properties = new HashMap<>(additionalProperties);
		Project project = getProject();
		properties.put("hash", sha256(this.archive.get().getAsFile()));
		properties.put("repo", ArtifactRelease.forProject(project).getDownloadRepo());
		properties.put("project", project);
		return properties;
	}

	/**
	 * Calculates the SHA-256 hash value of a given file.
	 * @param file the file for which the SHA-256 hash value needs to be calculated
	 * @return the SHA-256 hash value of the file as a hexadecimal string
	 * @throws TaskExecutionException if an error occurs during the calculation of the
	 * hash value
	 */
	private String sha256(File file) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return new DigestUtils(digest).digestAsHex(file);
		}
		catch (Exception ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	/**
	 * Creates a formula by calling the {@link #createDescriptor(Map)} method with an
	 * empty map.
	 *
	 * @see HomebrewFormula#createDescriptor(Map)
	 */
	@TaskAction
	void createFormula() {
		createDescriptor(Collections.emptyMap());
	}

}
