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

package org.springframework.boot.build.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.jvm.Jvm;

/**
 * {@link Task} to run an application for the purpose of capturing its output for
 * inclusion in the reference documentation.
 *
 * @author Andy Wilkinson
 */
public class ApplicationRunner extends DefaultTask {

	private final RegularFileProperty output = getProject().getObjects().fileProperty();

	private final ListProperty<String> args = getProject().getObjects().listProperty(String.class);

	private final Property<String> mainClass = getProject().getObjects().property(String.class);

	private final Property<String> expectedLogging = getProject().getObjects().property(String.class);

	private final Property<String> applicationJar = getProject().getObjects()
		.property(String.class)
		.convention("/opt/apps/myapp.jar");

	private final Map<String, String> normalizations = new HashMap<>();

	private FileCollection classpath;

	@OutputFile
	public RegularFileProperty getOutput() {
		return this.output;
	}

	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}

	@Input
	public ListProperty<String> getArgs() {
		return this.args;
	}

	@Input
	public Property<String> getMainClass() {
		return this.mainClass;
	}

	@Input
	public Property<String> getExpectedLogging() {
		return this.expectedLogging;
	}

	@Input
	Map<String, String> getNormalizations() {
		return this.normalizations;
	}

	@Input
	public Property<String> getApplicationJar() {
		return this.applicationJar;
	}

	public void normalizeTomcatPort() {
		this.normalizations.put("(Tomcat started on port )[\\d]+( \\(http\\))", "$18080$2");
		this.normalizations.put("(Tomcat initialized with port )[\\d]+( \\(http\\))", "$18080$2");
	}

	public void normalizeLiveReloadPort() {
		this.normalizations.put("(LiveReload server is running on port )[\\d]+", "$135729");
	}

	@TaskAction
	void runApplication() throws IOException {
		List<String> command = new ArrayList<>();
		File executable = Jvm.current().getExecutable("java");
		command.add(executable.getAbsolutePath());
		command.add("-cp");
		command.add(this.classpath.getFiles()
			.stream()
			.map(File::getAbsolutePath)
			.collect(Collectors.joining(File.pathSeparator)));
		command.add(this.mainClass.get());
		command.addAll(this.args.get());
		File outputFile = this.output.getAsFile().get();
		Process process = new ProcessBuilder().redirectOutput(outputFile)
			.redirectError(outputFile)
			.command(command)
			.start();
		awaitLogging(process);
		process.destroy();
		normalizeLogging();
	}

	private void awaitLogging(Process process) {
		long end = System.currentTimeMillis() + 60000;
		String expectedLogging = this.expectedLogging.get();
		while (System.currentTimeMillis() < end) {
			for (String line : outputLines()) {
				if (line.contains(expectedLogging)) {
					return;
				}
			}
			if (!process.isAlive()) {
				throw new IllegalStateException("Process exited before '" + expectedLogging + "' was logged");
			}
		}
		throw new IllegalStateException("'" + expectedLogging + "' was not logged within 60 seconds");
	}

	private List<String> outputLines() {
		Path outputPath = this.output.get().getAsFile().toPath();
		try {
			return Files.readAllLines(outputPath);
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read lines of output from '" + outputPath + "'", ex);
		}
	}

	private void normalizeLogging() {
		List<String> outputLines = outputLines();
		List<String> normalizedLines = normalize(outputLines);
		Path outputPath = this.output.get().getAsFile().toPath();
		try {
			Files.write(outputPath, normalizedLines);
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to write normalized lines of output to '" + outputPath + "'", ex);
		}
	}

	private List<String> normalize(List<String> lines) {
		List<String> normalizedLines = lines;
		Map<String, String> normalizations = new HashMap<>(this.normalizations);
		normalizations.put("(Starting .* using Java .* with PID [\\d]+ \\().*( started by ).*( in ).*(\\))",
				"$1" + this.applicationJar.get() + "$2myuser$3/opt/apps/$4");
		for (Entry<String, String> normalization : normalizations.entrySet()) {
			Pattern pattern = Pattern.compile(normalization.getKey());
			normalizedLines = normalize(normalizedLines, pattern, normalization.getValue());
		}
		return normalizedLines;
	}

	private List<String> normalize(List<String> lines, Pattern pattern, String replacement) {
		boolean matched = false;
		List<String> normalizedLines = new ArrayList<>();
		for (String line : lines) {
			Matcher matcher = pattern.matcher(line);
			StringBuilder transformed = new StringBuilder();
			while (matcher.find()) {
				matched = true;
				matcher.appendReplacement(transformed, replacement);
			}
			matcher.appendTail(transformed);
			normalizedLines.add(transformed.toString());
		}
		if (!matched) {
			reportUnmatchedNormalization(lines, pattern);
		}
		return normalizedLines;
	}

	private void reportUnmatchedNormalization(List<String> lines, Pattern pattern) {
		StringBuilder message = new StringBuilder(
				"'" + pattern + "' did not match any of the following lines of output:");
		message.append(String.format("%n"));
		for (String line : lines) {
			message.append(String.format("%s%n", line));
		}
		throw new IllegalStateException(message.toString());
	}

}
