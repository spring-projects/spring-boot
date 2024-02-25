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

package org.springframework.boot.build.starters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.springframework.core.CollectionFactory;

/**
 * A {@link Task} for generating metadata that describes a starter.
 *
 * @author Andy Wilkinson
 */
public class StarterMetadata extends DefaultTask {

	private Configuration dependencies;

	private File destination;

	/**
     * Initializes the StarterMetadata object.
     * Sets the name and description properties based on the project's name and description.
     * 
     * @param name a Callable that returns the project's name
     * @param description a Callable that returns the project's description
     */
    public StarterMetadata() {
		getInputs().property("name", (Callable<String>) () -> getProject().getName());
		getInputs().property("description", (Callable<String>) () -> getProject().getDescription());
	}

	/**
     * Returns the dependencies of the StarterMetadata.
     *
     * @return the dependencies of the StarterMetadata
     */
    @Classpath
	public FileCollection getDependencies() {
		return this.dependencies;
	}

	/**
     * Sets the dependencies for the StarterMetadata.
     * 
     * @param dependencies the configuration object representing the dependencies
     */
    public void setDependencies(Configuration dependencies) {
		this.dependencies = dependencies;
	}

	/**
     * Returns the destination file.
     *
     * @return the destination file
     */
    @OutputFile
	public File getDestination() {
		return this.destination;
	}

	/**
     * Sets the destination file for the StarterMetadata.
     * 
     * @param destination the destination file to be set
     */
    public void setDestination(File destination) {
		this.destination = destination;
	}

	/**
     * Generates metadata for the StarterMetadata class.
     * This method creates a properties object and sets various properties such as name, description, and dependencies.
     * It then creates the necessary directories for the destination file and writes the properties to the file.
     * 
     * @throws IOException if an I/O error occurs while writing the properties to the file
     */
    @TaskAction
	void generateMetadata() throws IOException {
		Properties properties = CollectionFactory.createSortedProperties(true);
		properties.setProperty("name", getProject().getName());
		properties.setProperty("description", getProject().getDescription());
		properties.setProperty("dependencies",
				String.join(",",
						this.dependencies.getResolvedConfiguration()
							.getResolvedArtifacts()
							.stream()
							.map(ResolvedArtifact::getName)
							.collect(Collectors.toSet())));
		this.destination.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(this.destination)) {
			properties.store(writer, null);
		}
	}

}
