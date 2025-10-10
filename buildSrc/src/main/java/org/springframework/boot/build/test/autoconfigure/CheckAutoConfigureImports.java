/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.boot.build.test.autoconfigure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.gradle.api.DefaultTask;
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

import org.springframework.boot.build.autoconfigure.AutoConfigurationClass;

/**
 * Task to check the contents of a project's
 * {@code META-INF/spring/*.AutoConfigure*.imports} files.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckAutoConfigureImports extends DefaultTask {

	private FileCollection sourceFiles = getProject().getObjects().fileCollection();

	private FileCollection classpath = getProject().getObjects().fileCollection();

	public CheckAutoConfigureImports() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
	}

	@InputFiles
	@SkipWhenEmpty
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return this.sourceFiles.getAsFileTree()
			.matching((filter) -> filter.include("META-INF/spring/*.AutoConfigure*.imports"));
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
		Map<String, List<String>> allProblems = new TreeMap<>();
		for (AutoConfigureImports autoConfigureImports : loadImports()) {
			List<String> problems = new ArrayList<>();
			if (!find(autoConfigureImports.annotationName)) {
				problems.add("Annotation '%s' was not found".formatted(autoConfigureImports.annotationName));
			}
			for (String imported : autoConfigureImports.imports) {
				String importedClassName = imported;
				if (importedClassName.startsWith("optional:")) {
					importedClassName = importedClassName.substring("optional:".length());
				}
				boolean found = find(importedClassName, (input) -> {
					if (!correctlyAnnotated(input)) {
						problems.add("Imported auto-configuration '%s' is not annotated with @AutoConfiguration"
							.formatted(imported));
					}
				});
				if (!found) {
					problems.add("Imported auto-configuration '%s' was not found".formatted(importedClassName));
				}

			}
			List<String> sortedValues = new ArrayList<>(autoConfigureImports.imports);
			Collections.sort(sortedValues, (i1, i2) -> {
				boolean imported1 = i1.startsWith("optional:");
				boolean imported2 = i2.startsWith("optional:");
				int comparison = Boolean.compare(imported1, imported2);
				if (comparison != 0) {
					return comparison;
				}
				return i1.compareTo(i2);
			});
			if (!sortedValues.equals(autoConfigureImports.imports)) {
				File sortedOutputFile = getOutputDirectory().file("sorted-" + autoConfigureImports.fileName)
					.get()
					.getAsFile();
				writeString(sortedOutputFile, sortedValues.stream().collect(Collectors.joining(System.lineSeparator()))
						+ System.lineSeparator());
				problems.add(
						"Entries should be required then optional, each sorted alphabetically (expected content written to '%s')"
							.formatted(sortedOutputFile.getAbsolutePath()));
			}
			if (!problems.isEmpty()) {
				allProblems.computeIfAbsent(autoConfigureImports.fileName, (unused) -> new ArrayList<>())
					.addAll(problems);
			}
		}
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		writeReport(allProblems, outputFile);
		if (!allProblems.isEmpty()) {
			throw new VerificationException(
					"AutoConfigure….imports checks failed. See '%s' for details".formatted(outputFile));
		}
	}

	private List<AutoConfigureImports> loadImports() {
		return getSource().getFiles().stream().map((file) -> {
			String fileName = file.getName();
			String annotationName = fileName.substring(0, fileName.length() - ".imports".length());
			return new AutoConfigureImports(annotationName, loadImports(file), fileName);
		}).toList();
	}

	private List<String> loadImports(File importsFile) {
		try {
			return Files.readAllLines(importsFile.toPath());
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private boolean find(String className) {
		return find(className, (input) -> {
		});
	}

	private boolean find(String className, Consumer<InputStream> handler) {
		for (File root : this.classpath.getFiles()) {
			String classFilePath = className.replace(".", "/") + ".class";
			if (root.isDirectory()) {
				File classFile = new File(root, classFilePath);
				if (classFile.isFile()) {
					try (InputStream input = new FileInputStream(classFile)) {
						handler.accept(input);
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
					return true;
				}
			}
			else {
				try (JarFile jar = new JarFile(root)) {
					ZipEntry entry = jar.getEntry(classFilePath);
					if (entry != null) {
						try (InputStream input = jar.getInputStream(entry)) {
							handler.accept(input);
						}
						return true;
					}
				}
				catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
			}
		}
		return false;
	}

	private boolean correctlyAnnotated(InputStream classFile) {
		return AutoConfigurationClass.of(classFile) != null;
	}

	private void writeReport(Map<String, List<String>> allProblems, File outputFile) {
		outputFile.getParentFile().mkdirs();
		StringBuilder report = new StringBuilder();
		if (!allProblems.isEmpty()) {
			allProblems.forEach((fileName, problems) -> {
				report.append("Found problems in '%s':%n".formatted(fileName));
				problems.forEach((problem) -> report.append("  - %s%n".formatted(problem)));
			});
		}
		writeString(outputFile, report.toString());
	}

	private void writeString(File file, String content) {
		try {
			Files.writeString(file.toPath(), content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	record AutoConfigureImports(String annotationName, List<String> imports, String fileName) {

	}

}
