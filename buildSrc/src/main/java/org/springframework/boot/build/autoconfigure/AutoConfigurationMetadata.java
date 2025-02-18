/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.build.autoconfigure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import org.springframework.asm.ClassReader;
import org.springframework.asm.Opcodes;
import org.springframework.core.CollectionFactory;

/**
 * A {@link Task} for generating metadata describing a project's auto-configuration
 * classes.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public abstract class AutoConfigurationMetadata extends DefaultTask {

	private static final String COMMENT_START = "#";

	private final String moduleName;

	private FileCollection classesDirectories;

	public AutoConfigurationMetadata() {
		getProject().getConfigurations()
			.maybeCreate(AutoConfigurationPlugin.AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME);
		this.moduleName = getProject().getName();
	}

	public void setSourceSet(SourceSet sourceSet) {
		getAutoConfigurationImports().set(new File(sourceSet.getOutput().getResourcesDir(),
				"META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
		this.classesDirectories = sourceSet.getOutput().getClassesDirs();
		dependsOn(sourceSet.getOutput());
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	abstract RegularFileProperty getAutoConfigurationImports();

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@Classpath
	FileCollection getClassesDirectories() {
		return this.classesDirectories;
	}

	@TaskAction
	void documentAutoConfiguration() throws IOException {
		Properties autoConfiguration = readAutoConfiguration();
		File outputFile = getOutputFile().get().getAsFile();
		outputFile.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(outputFile)) {
			autoConfiguration.store(writer, null);
		}
	}

	private Properties readAutoConfiguration() throws IOException {
		Properties autoConfiguration = CollectionFactory.createSortedProperties(true);
		List<String> classNames = readAutoConfigurationsFile();
		Set<String> publicClassNames = new LinkedHashSet<>();
		for (String className : classNames) {
			File classFile = findClassFile(className);
			if (classFile == null) {
				throw new IllegalStateException("Auto-configuration class '" + className + "' not found.");
			}
			try (InputStream in = new FileInputStream(classFile)) {
				int access = new ClassReader(in).getAccess();
				if ((access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
					publicClassNames.add(className);
				}
			}
		}
		autoConfiguration.setProperty("autoConfigurationClassNames", String.join(",", publicClassNames));
		autoConfiguration.setProperty("module", this.moduleName);
		return autoConfiguration;
	}

	/**
	 * Reads auto-configurations from
	 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
	 * @return auto-configurations
	 */
	private List<String> readAutoConfigurationsFile() throws IOException {
		File file = getAutoConfigurationImports().getAsFile().get();
		if (!file.exists()) {
			return Collections.emptyList();
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			return reader.lines().map(this::stripComment).filter((line) -> !line.isEmpty()).toList();
		}
	}

	private String stripComment(String line) {
		int commentStart = line.indexOf(COMMENT_START);
		if (commentStart == -1) {
			return line.trim();
		}
		return line.substring(0, commentStart).trim();
	}

	private File findClassFile(String className) {
		String classFileName = className.replace(".", "/") + ".class";
		for (File classesDir : this.classesDirectories) {
			File classFile = new File(classesDir, classFileName);
			if (classFile.isFile()) {
				return classFile;
			}
		}
		return null;
	}

}
