/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Task to check the contents of a project's
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * file.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckAutoConfigurationImports extends AutoConfigurationImportsTask {

	private FileCollection classpath = getProject().getObjects().fileCollection();

	public CheckAutoConfigurationImports() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
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
		File importsFile = getSource().getSingleFile();
		check(importsFile);
	}

	private void check(File importsFile) {
		List<String> imports = loadImports();
		List<String> problems = new ArrayList<>();
		for (String imported : imports) {
			File classFile = find(imported);
			if (classFile == null) {
				problems.add("'%s' was not found".formatted(imported));
			}
			else if (!correctlyAnnotated(classFile)) {
				problems.add("'%s' is not annotated with @AutoConfiguration".formatted(imported));
			}
		}
		List<String> sortedValues = new ArrayList<>(imports);
		Collections.sort(sortedValues);
		if (!sortedValues.equals(imports)) {
			File sortedOutputFile = getOutputDirectory().file("sorted-" + importsFile.getName()).get().getAsFile();
			writeString(sortedOutputFile,
					sortedValues.stream().collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator());
			problems.add("Entries should be sorted alphabetically (expect content written to "
					+ sortedOutputFile.getAbsolutePath() + ")");
		}
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		writeReport(importsFile, problems, outputFile);
		if (!problems.isEmpty()) {
			throw new VerificationException("%s check failed. See '%s' for details"
				.formatted(AutoConfigurationImportsTask.IMPORTS_FILE, outputFile));
		}
	}

	private File find(String className) {
		for (File root : this.classpath.getFiles()) {
			String classFilePath = className.replace(".", "/") + ".class";
			File classFile = new File(root, classFilePath);
			if (classFile.isFile()) {
				return classFile;
			}
		}
		return null;
	}

	private boolean correctlyAnnotated(File classFile) {
		return AutoConfigurationClass.of(classFile) != null;
	}

	private void writeReport(File importsFile, List<String> problems, File outputFile) {
		outputFile.getParentFile().mkdirs();
		StringBuilder report = new StringBuilder();
		if (!problems.isEmpty()) {
			report.append("Found problems in '%s':%n".formatted(importsFile));
			problems.forEach((problem) -> report.append("  - %s%n".formatted(problem)));
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

}
