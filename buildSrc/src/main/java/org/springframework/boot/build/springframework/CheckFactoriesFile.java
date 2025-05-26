/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.build.springframework;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.StringUtils;

/**
 * {@link Task} that checks files loaded by {@link SpringFactoriesLoader}.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckFactoriesFile extends DefaultTask {

	private final String path;

	private FileCollection sourceFiles = getProject().getObjects().fileCollection();

	private FileCollection classpath = getProject().getObjects().fileCollection();

	protected CheckFactoriesFile(String path) {
		this.path = path;
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
	}

	@InputFiles
	@SkipWhenEmpty
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return this.sourceFiles.getAsFileTree().matching((filter) -> filter.include(this.path));
	}

	public void setSource(Object source) {
		this.sourceFiles = getProject().getObjects().fileCollection().from(source);
	}

	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	public void setClasspath(Object classpath) {
		this.classpath = getProject().getObjects().fileCollection().from(classpath);
	}

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@TaskAction
	void execute() {
		getSource().forEach(this::check);
	}

	private void check(File factoriesFile) {
		Properties properties = load(factoriesFile);
		Map<String, List<String>> problems = new LinkedHashMap<>();
		for (String name : properties.stringPropertyNames()) {
			String value = properties.getProperty(name);
			List<String> classNames = Arrays.asList(StringUtils.commaDelimitedListToStringArray(value));
			collectProblems(problems, name, classNames);
			List<String> sortedValues = new ArrayList<>(classNames);
			Collections.sort(sortedValues);
			if (!sortedValues.equals(classNames)) {
				List<String> problemsForClassName = problems.computeIfAbsent(name, (k) -> new ArrayList<>());
				problemsForClassName.add("Entries should be sorted alphabetically");
			}
		}
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		writeReport(factoriesFile, problems, outputFile);
		if (!problems.isEmpty()) {
			throw new VerificationException("%s check failed. See '%s' for details".formatted(this.path, outputFile));
		}
	}

	private void collectProblems(Map<String, List<String>> problems, String key, List<String> classNames) {
		for (String className : classNames) {
			if (!find(className)) {
				addNoFoundProblem(className, problems.computeIfAbsent(key, (k) -> new ArrayList<>()));
			}
		}
	}

	private void addNoFoundProblem(String className, List<String> problemsForClassName) {
		String binaryName = binaryNameOf(className);
		boolean foundBinaryForm = find(binaryName);
		problemsForClassName.add(!foundBinaryForm ? "'%s' was not found".formatted(className)
				: "'%s' should be listed using its binary name '%s'".formatted(className, binaryName));
	}

	private boolean find(String className) {
		for (File root : this.classpath.getFiles()) {
			String classFilePath = className.replace(".", "/") + ".class";
			if (new File(root, classFilePath).isFile()) {
				return true;
			}
		}
		return false;
	}

	private String binaryNameOf(String className) {
		int lastDotIndex = className.lastIndexOf('.');
		return className.substring(0, lastDotIndex) + "$" + className.substring(lastDotIndex + 1);
	}

	private Properties load(File aotFactories) {
		Properties properties = new Properties();
		try (FileInputStream input = new FileInputStream(aotFactories)) {
			properties.load(input);
			return properties;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void writeReport(File factoriesFile, Map<String, List<String>> problems, File outputFile) {
		outputFile.getParentFile().mkdirs();
		StringBuilder report = new StringBuilder();
		if (!problems.isEmpty()) {
			report.append("Found problems in '%s':%n".formatted(factoriesFile));
			problems.forEach((key, problemsForKey) -> {
				report.append("  - %s:%n".formatted(key));
				problemsForKey.forEach((problem) -> report.append("    - %s%n".formatted(problem)));
			});
		}
		try {
			Files.writeString(outputFile.toPath(), report.toString(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
