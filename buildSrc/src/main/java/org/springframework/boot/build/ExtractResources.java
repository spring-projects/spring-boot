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

	public ExtractResources() {
		this.destinationDirectory = getProject().getObjects().directoryProperty();
	}

	@Input
	public List<String> getResourceNames() {
		return this.resourceNames;
	}

	public void setResourcesNames(List<String> resourceNames) {
		this.resourceNames = resourceNames;
	}

	@OutputDirectory
	public DirectoryProperty getDestinationDirectory() {
		return this.destinationDirectory;
	}

	public void property(String name, String value) {
		this.properties.put(name, value);
	}

	@Input
	public Map<String, String> getProperties() {
		return this.properties;
	}

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
