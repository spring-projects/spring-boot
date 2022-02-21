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

package org.springframework.boot.build.autoconfigure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import org.springframework.asm.ClassReader;
import org.springframework.asm.Opcodes;
import org.springframework.core.CollectionFactory;
import org.springframework.util.StringUtils;

/**
 * A {@link Task} for generating metadata describing a project's auto-configuration
 * classes.
 *
 * @author Andy Wilkinson
 */
public class AutoConfigurationMetadata extends DefaultTask {

	private static final String COMMENT_START = "#";

	private SourceSet sourceSet;

	private File outputFile;

	public AutoConfigurationMetadata() {
		getInputs()
				.file((Callable<File>) () -> new File(this.sourceSet.getOutput().getResourcesDir(),
						"META-INF/spring.factories"))
				.withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("spring.factories");
		getInputs()
				.file((Callable<File>) () -> new File(this.sourceSet.getOutput().getResourcesDir(),
						"META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"))
				.withPathSensitivity(PathSensitivity.RELATIVE)
				.withPropertyName("org.springframework.boot.autoconfigure.AutoConfiguration");

		dependsOn((Callable<String>) () -> this.sourceSet.getProcessResourcesTaskName());
		getProject().getConfigurations()
				.maybeCreate(AutoConfigurationPlugin.AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME);
	}

	public void setSourceSet(SourceSet sourceSet) {
		this.sourceSet = sourceSet;
	}

	@OutputFile
	public File getOutputFile() {
		return this.outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	@TaskAction
	void documentAutoConfiguration() throws IOException {
		Properties autoConfiguration = readAutoConfiguration();
		getOutputFile().getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(getOutputFile())) {
			autoConfiguration.store(writer, null);
		}
	}

	private Properties readAutoConfiguration() throws IOException {
		Properties autoConfiguration = CollectionFactory.createSortedProperties(true);
		Set<String> classNames = new LinkedHashSet<>();
		classNames.addAll(readSpringFactories());
		classNames.addAll(readAutoConfigurationsFile());
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
		autoConfiguration.setProperty("module", getProject().getName());
		return autoConfiguration;
	}

	/**
	 * Reads auto-configurations from META-INF/spring.factories.
	 * @return auto-configurations
	 */
	private Set<String> readSpringFactories() throws IOException {
		File file = new File(this.sourceSet.getOutput().getResourcesDir(), "META-INF/spring.factories");
		if (!file.exists()) {
			return Collections.emptySet();
		}
		Properties springFactories = readSpringFactories(file);
		String enableAutoConfiguration = springFactories
				.getProperty("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
		return StringUtils.commaDelimitedListToSet(enableAutoConfiguration);
	}

	/**
	 * Reads auto-configurations from
	 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
	 * @return auto-configurations
	 */
	private List<String> readAutoConfigurationsFile() throws IOException {
		File file = new File(this.sourceSet.getOutput().getResourcesDir(),
				"META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
		if (!file.exists()) {
			return Collections.emptyList();
		}
		// Nearly identical copy of
		// org.springframework.boot.context.annotation.ImportCandidates.load
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			List<String> autoConfigurations = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				line = stripComment(line);
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				autoConfigurations.add(line);
			}
			return autoConfigurations;
		}
	}

	private String stripComment(String line) {
		int commentStart = line.indexOf(COMMENT_START);
		if (commentStart == -1) {
			return line;
		}
		return line.substring(0, commentStart);
	}

	private File findClassFile(String className) {
		String classFileName = className.replace(".", "/") + ".class";
		for (File classesDir : this.sourceSet.getOutput().getClassesDirs()) {
			File classFile = new File(classesDir, classFileName);
			if (classFile.isFile()) {
				return classFile;
			}
		}
		return null;
	}

	private Properties readSpringFactories(File file) throws IOException {
		Properties springFactories = new Properties();
		try (Reader in = new FileReader(file)) {
			springFactories.load(in);
		}
		return springFactories;
	}

}
