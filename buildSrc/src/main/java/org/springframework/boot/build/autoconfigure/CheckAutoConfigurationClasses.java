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

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Task to check a project's {@code @AutoConfiguration} classes.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckAutoConfigurationClasses extends AutoConfigurationImportsTask {

	private FileCollection classpath = getProject().getObjects().fileCollection();

	private FileCollection optionalDependencies = getProject().getObjects().fileCollection();

	private FileCollection requiredDependencies = getProject().getObjects().fileCollection();

	private SetProperty<String> optionalDependencyClassNames = getProject().getObjects().setProperty(String.class);

	private SetProperty<String> requiredDependencyClassNames = getProject().getObjects().setProperty(String.class);

	public CheckAutoConfigurationClasses() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
		this.optionalDependencyClassNames.set(getProject().provider(() -> classNamesOf(this.optionalDependencies)));
		this.requiredDependencyClassNames.set(getProject().provider(() -> classNamesOf(this.requiredDependencies)));
	}

	private static List<String> classNamesOf(FileCollection classpath) {
		return classpath.getFiles().stream().flatMap((file) -> {
			try (JarFile jarFile = new JarFile(file)) {
				return Collections.list(jarFile.entries())
					.stream()
					.filter((entry) -> !entry.isDirectory())
					.map(JarEntry::getName)
					.filter((entryName) -> entryName.endsWith(".class"))
					.map((entryName) -> entryName.substring(0, entryName.length() - ".class".length()))
					.map((entryName) -> entryName.replace("/", "."));
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}).toList();
	}

	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	public void setClasspath(Object classpath) {
		this.classpath = getProject().getObjects().fileCollection().from(classpath);
	}

	@Classpath
	public FileCollection getOptionalDependencies() {
		return this.optionalDependencies;
	}

	public void setOptionalDependencies(Object classpath) {
		this.optionalDependencies = getProject().getObjects().fileCollection().from(classpath);
	}

	@Classpath
	public FileCollection getRequiredDependencies() {
		return this.requiredDependencies;
	}

	public void setRequiredDependencies(Object classpath) {
		this.requiredDependencies = getProject().getObjects().fileCollection().from(classpath);
	}

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@TaskAction
	void execute() {
		Map<String, List<String>> problems = new TreeMap<>();
		Set<String> optionalOnlyClassNames = new HashSet<>(this.optionalDependencyClassNames.get());
		Set<String> requiredClassNames = this.requiredDependencyClassNames.get();
		optionalOnlyClassNames.removeAll(requiredClassNames);
		classFiles().forEach((classFile) -> {
			AutoConfigurationClass autoConfigurationClass = AutoConfigurationClass.of(classFile);
			if (autoConfigurationClass != null) {
				check(autoConfigurationClass, optionalOnlyClassNames, requiredClassNames, problems);
			}
		});
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		writeReport(problems, outputFile);
		if (!problems.isEmpty()) {
			throw new VerificationException(
					"Auto-configuration class check failed. See '%s' for details".formatted(outputFile));
		}
	}

	private List<File> classFiles() {
		List<File> classFiles = new ArrayList<>();
		for (File root : this.classpath.getFiles()) {
			try (Stream<Path> files = Files.walk(root.toPath())) {
				files.forEach((file) -> {
					if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(".class")) {
						classFiles.add(file.toFile());
					}
				});
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		return classFiles;
	}

	private void check(AutoConfigurationClass autoConfigurationClass, Set<String> optionalOnlyClassNames,
			Set<String> requiredClassNames, Map<String, List<String>> problems) {
		if (!autoConfigurationClass.name().endsWith("AutoConfiguration")) {
			problems.computeIfAbsent(autoConfigurationClass.name(), (name) -> new ArrayList<>())
				.add("Name of a class annotated with @AutoConfiguration should end with AutoConfiguration");
		}
		autoConfigurationClass.before().forEach((before) -> {
			if (optionalOnlyClassNames.contains(before)) {
				problems.computeIfAbsent(autoConfigurationClass.name(), (name) -> new ArrayList<>())
					.add("before '%s' is from an optional dependency and should be declared in beforeName"
						.formatted(before));
			}
		});
		autoConfigurationClass.beforeName().forEach((beforeName) -> {
			if (!optionalOnlyClassNames.contains(beforeName)) {
				String problem = requiredClassNames.contains(beforeName)
						? "beforeName '%s' is from a required dependency and should be declared in before"
							.formatted(beforeName)
						: "beforeName '%s' not found".formatted(beforeName);
				problems.computeIfAbsent(autoConfigurationClass.name(), (name) -> new ArrayList<>()).add(problem);
			}
		});
		autoConfigurationClass.after().forEach((after) -> {
			if (optionalOnlyClassNames.contains(after)) {
				problems.computeIfAbsent(autoConfigurationClass.name(), (name) -> new ArrayList<>())
					.add("after '%s' is from an optional dependency and should be declared in afterName"
						.formatted(after));
			}
		});
		autoConfigurationClass.afterName().forEach((afterName) -> {
			if (!optionalOnlyClassNames.contains(afterName)) {
				String problem = requiredClassNames.contains(afterName)
						? "afterName '%s' is from a required dependency and should be declared in after"
							.formatted(afterName)
						: "afterName '%s' not found".formatted(afterName);
				problems.computeIfAbsent(autoConfigurationClass.name(), (name) -> new ArrayList<>()).add(problem);
			}
		});
	}

	private void writeReport(Map<String, List<String>> problems, File outputFile) {
		outputFile.getParentFile().mkdirs();
		StringBuilder report = new StringBuilder();
		if (!problems.isEmpty()) {
			report.append("Found auto-configuration class problems:%n".formatted());
			problems.forEach((className, classProblems) -> {
				report.append("  - %s:%n".formatted(className));
				classProblems.forEach((problem) -> report.append("    - %s%n".formatted(problem)));
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
