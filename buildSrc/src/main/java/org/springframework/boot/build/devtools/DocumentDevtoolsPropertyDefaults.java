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

package org.springframework.boot.build.devtools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Task for documenting Devtools' property defaults.
 *
 * @author Andy Wilkinson
 */
public class DocumentDevtoolsPropertyDefaults extends DefaultTask {

	private final Configuration devtools;

	private final RegularFileProperty outputFile;

	public DocumentDevtoolsPropertyDefaults() {
		this.devtools = getProject().getConfigurations().create("devtools");
		this.outputFile = getProject().getObjects().fileProperty();
		this.outputFile.convention(getProject().getLayout().getBuildDirectory()
				.file("docs/generated/using/devtools-property-defaults.adoc"));
		Map<String, String> dependency = new HashMap<>();
		dependency.put("path", ":spring-boot-project:spring-boot-devtools");
		dependency.put("configuration", "propertyDefaults");
		this.devtools.getDependencies().add(getProject().getDependencies().project(dependency));
	}

	@InputFiles
	public FileCollection getDevtools() {
		return this.devtools;
	}

	@OutputFile
	public RegularFileProperty getOutputFile() {
		return this.outputFile;
	}

	@TaskAction
	void documentPropertyDefaults() throws IOException {
		Map<String, String> properties = loadProperties();
		documentProperties(properties);
	}

	private Map<String, String> loadProperties() throws IOException, FileNotFoundException {
		Properties properties = new Properties();
		Map<String, String> sortedProperties = new TreeMap<>();
		try (FileInputStream stream = new FileInputStream(this.devtools.getSingleFile())) {
			properties.load(stream);
			for (String name : properties.stringPropertyNames()) {
				sortedProperties.put(name, properties.getProperty(name));
			}
		}
		return sortedProperties;
	}

	private void documentProperties(Map<String, String> properties) throws IOException {
		try (PrintWriter writer = new PrintWriter(new FileWriter(this.outputFile.getAsFile().get()))) {
			writer.println("[cols=\"3,1\"]");
			writer.println("|===");
			writer.println("| Name | Default Value");
			properties.forEach((name, value) -> {
				writer.println();
				writer.printf("| `%s`%n", name);
				writer.printf("| `%s`%n", value);
			});
			writer.println("|===");
		}
	}

}
