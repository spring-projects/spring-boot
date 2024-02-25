/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.build;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * {@link Task} to extract resources from the classpath and write them to disk.
 *
 * @author Andy Wilkinson
 */
public class ExtractResources extends DefaultTask {

	private final PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");

	private final Map<String, String> properties = new HashMap<>();

	private final DirectoryProperty destinationDirectory;

	private List<String> resourceNames = new ArrayList<>();

	/**
     * Initializes the destination directory for extracting resources.
     */
    public ExtractResources() {
		this.destinationDirectory = getProject().getObjects().directoryProperty();
	}

	/**
     * Returns the list of resource names.
     *
     * @return the list of resource names
     */
    @Input
	public List<String> getResourceNames() {
		return this.resourceNames;
	}

	/**
     * Sets the names of the resources.
     * 
     * @param resourceNames the list of resource names to be set
     */
    public void setResourcesNames(List<String> resourceNames) {
		this.resourceNames = resourceNames;
	}

	/**
     * Returns the destination directory where the extracted resources will be stored.
     *
     * @return The destination directory.
     */
    @OutputDirectory
	public DirectoryProperty getDestinationDirectory() {
		return this.destinationDirectory;
	}

	/**
     * Sets the value of a property with the given name.
     * 
     * @param name the name of the property
     * @param value the value to be set for the property
     */
    public void property(String name, String value) {
		this.properties.put(name, value);
	}

	/**
     * Returns the properties of the ExtractResources object.
     *
     * @return a Map containing the properties of the ExtractResources object
     */
    @Input
	public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
     * Extracts the resources specified in the resourceNames list and copies them to the destination directory.
     * 
     * @throws IOException if an I/O error occurs while reading or writing the resources
     * @throws GradleException if a resource does not exist
     */
    @TaskAction
	void extractResources() throws IOException {
		for (String resourceName : this.resourceNames) {
			InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourceName);
			if (resourceStream == null) {
				throw new GradleException("Resource '" + resourceName + "' does not exist");
			}
			String resource = FileCopyUtils.copyToString(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
			resource = this.propertyPlaceholderHelper.replacePlaceholders(resource, this.properties::get);
			FileCopyUtils.copy(resource,
					new FileWriter(this.destinationDirectory.file(resourceName).get().getAsFile()));
		}
	}

}
