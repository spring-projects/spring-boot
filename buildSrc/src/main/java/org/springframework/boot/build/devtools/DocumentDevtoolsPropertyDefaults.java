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

	/**
	 * Initializes the devtools property defaults for the document.
	 *
	 * This method creates the devtools configuration and sets the output file for the
	 * generated documentation. The default output file location is
	 * "build/docs/generated/using/devtools-property-defaults.adoc".
	 *
	 * It also adds the "spring-boot-devtools" project as a dependency to the devtools
	 * configuration with the "propertyDefaults" configuration.
	 */
	public DocumentDevtoolsPropertyDefaults() {
		this.devtools = getProject().getConfigurations().create("devtools");
		this.outputFile = getProject().getObjects().fileProperty();
		this.outputFile.convention(getProject().getLayout()
			.getBuildDirectory()
			.file("docs/generated/using/devtools-property-defaults.adoc"));
		Map<String, String> dependency = new HashMap<>();
		dependency.put("path", ":spring-boot-project:spring-boot-devtools");
		dependency.put("configuration", "propertyDefaults");
		this.devtools.getDependencies().add(getProject().getDependencies().project(dependency));
	}

	/**
	 * Returns the collection of devtools files.
	 * @return the collection of devtools files
	 */
	@InputFiles
	public FileCollection getDevtools() {
		return this.devtools;
	}

	/**
	 * Returns the output file property.
	 * @return the output file property
	 */
	@OutputFile
	public RegularFileProperty getOutputFile() {
		return this.outputFile;
	}

	/**
	 * This method is used to document the property defaults by loading the properties and
	 * documenting them using the JAVA Javadoc style.
	 * @throws IOException if there is an error while loading the properties
	 */
	@TaskAction
	void documentPropertyDefaults() throws IOException {
		Map<String, String> properties = loadProperties();
		documentProperties(properties);
	}

	/**
	 * Loads the properties from the devtools file and returns them as a sorted map.
	 * @return a map containing the properties, sorted by their keys
	 * @throws IOException if an I/O error occurs while reading the file
	 * @throws FileNotFoundException if the devtools file is not found
	 */
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

	/**
	 * Generates a Javadoc style documentation comment for the {@code documentProperties}
	 * method. This method is declared in the {@code DocumentDevtoolsPropertyDefaults}
	 * class.
	 * @param properties a {@code Map} containing the properties to be documented
	 * @throws IOException if an I/O error occurs while writing the documentation
	 */
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
