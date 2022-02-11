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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} to generate properties and write them to disk as a properties file.
 *
 * @author Scott Frederick
 */
public class GeneratePropertiesResource extends DefaultTask {

	private final Property<String> propertiesFileName;

	private final DirectoryProperty destinationDirectory;

	private final Map<String, String> properties = new HashMap<>();

	public GeneratePropertiesResource() {
		ObjectFactory objects = getProject().getObjects();
		this.propertiesFileName = objects.property(String.class);
		this.destinationDirectory = objects.directoryProperty();
	}

	@OutputDirectory
	public DirectoryProperty getDestinationDirectory() {
		return this.destinationDirectory;
	}

	@Input
	public Property<String> getPropertiesFileName() {
		return this.propertiesFileName;
	}

	public void property(String name, String value) {
		this.properties.put(name, value);
	}

	@Input
	public Map<String, String> getProperties() {
		return this.properties;
	}

	@TaskAction
	void generatePropertiesFile() throws IOException {
		File outputFile = this.destinationDirectory.file(this.propertiesFileName).get().getAsFile();
		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
			this.properties.forEach((key, value) -> writer.printf("%s=%s\n", key, value));
		}
	}

}
