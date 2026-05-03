/*
 * Copyright 2012-present the original author or authors.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Task for documenting Devtools' property defaults.
 *
 * @author Andy Wilkinson
 */
public abstract class DocumentDevtoolsPropertyDefaults extends DefaultTask {

	private FileCollection defaults;

	public DocumentDevtoolsPropertyDefaults() {
		getOutputFile().convention(getProject().getLayout()
			.getBuildDirectory()
			.file("generated/docs/using/devtools-property-defaults.adoc"));
	}

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getDefaults() {
		return this.defaults;
	}

	public void setDefaults(FileCollection defaults) {
		this.defaults = defaults;
	}

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@TaskAction
	void documentPropertyDefaults() throws IOException {
		Map<String, String> propertyDefaults = loadPropertyDefaults();
		documentPropertyDefaults(propertyDefaults);
	}

	private Map<String, String> loadPropertyDefaults() throws IOException, FileNotFoundException {
		Properties properties = new Properties();
		Map<String, String> propertyDefaults = new TreeMap<>();
		for (File contribution : this.defaults.getFiles()) {
			if (contribution.isFile()) {
				try (JarFile jar = new JarFile(contribution)) {
					ZipEntry entry = jar.getEntry("META-INF/spring-devtools.properties");
					if (entry != null) {
						properties.load(jar.getInputStream(entry));
					}
				}
			}
			else if (contribution.exists()) {
				throw new IllegalStateException(
						"Unexpected Devtools default properties contribution from '" + contribution + "'");
			}
		}
		for (String name : properties.stringPropertyNames()) {
			if (name.startsWith("defaults.")) {
				propertyDefaults.put(name.substring("defaults.".length()), properties.getProperty(name));
			}
		}
		return propertyDefaults;
	}

	private void documentPropertyDefaults(Map<String, String> properties) throws IOException {
		try (PrintWriter writer = new PrintWriter(new FileWriter(getOutputFile().getAsFile().get()))) {
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
