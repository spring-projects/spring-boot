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

	/**
	 * Returns the output file property.
	 * @return the output file property
	 */
	@OutputFile
	public RegularFileProperty getOutput() {
		return this.output;
	}

	/**
	 * Returns the classpath of the ApplicationRunner.
	 * @return the classpath of the ApplicationRunner
	 */
	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	/**
	 * Sets the classpath for the ApplicationRunner.
	 * @param classpath the classpath to be set
	 */
	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}

	/**
	 * Returns the list of arguments.
	 * @return the list of arguments
	 */
	@Input
	public ListProperty<String> getArgs() {
		return this.args;
	}

	/**
	 * Returns the main class property.
	 * @return the main class property
	 */
	@Input
	public Property<String> getMainClass() {
		return this.mainClass;
	}

	/**
	 * Retrieves the expected logging property.
	 * @return the expected logging property
	 */
	@Input
	public Property<String> getExpectedLogging() {
		return this.expectedLogging;
	}

	/**
	 * Returns the normalizations map.
	 * @return the normalizations map
	 */
	@Input
	Map<String, String> getNormalizations() {
		return this.normalizations;
	}

	/**
	 * Returns the application jar property.
	 * @return the application jar property
	 */
	@Input
	public Property<String> getApplicationJar() {
		return this.applicationJar;
	}

	/**
	 * Normalizes the Tomcat port.
	 *
	 * This method updates the normalizations map to replace the Tomcat port with a
	 * default port of 8080. The method searches for patterns in the form of "Tomcat
	 * started on port [port] (http)" or "Tomcat initialized with port [port] (http)" and
	 * replaces the [port] with the default port 8080.
	 * @return void
	 */
	public void normalizeTomcatPort() {
		this.normalizations.put("(Tomcat started on port )[\\d]+( \\(http\\))", "$18080$2");
		this.normalizations.put("(Tomcat initialized with port )[\\d]+( \\(http\\))", "$18080$2");
	}

	/**
	 * Normalizes the LiveReload port.
	 *
	 * This method updates the normalizations map to replace the LiveReload server port
	 * with a fixed value. The LiveReload server port is identified using a regular
	 * expression pattern and replaced with the value "$135729".
	 *
	 * @since 1.0
	 */
	public void normalizeLiveReloadPort() {
		this.normalizations.put("(LiveReload server is running on port )[\\d]+", "$135729");
	}

	/**
	 * Runs the application.
	 * @throws IOException if an I/O error occurs
	 */
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

	/**
	 * Waits for the specified logging message to be logged by the given process within a
	 * time limit of 60 seconds.
	 * @param process The process to monitor for logging messages.
	 * @throws IllegalStateException If the process exits before the expected logging
	 * message is logged.
	 * @throws IllegalStateException If the expected logging message is not logged within
	 * 60 seconds.
	 */
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

	/**
	 * Returns a list of strings representing the lines of output.
	 * @return a list of strings representing the lines of output
	 * @throws RuntimeException if failed to read lines of output from the specified
	 * output path
	 */
	private List<String> outputLines() {
		Path outputPath = this.output.get().getAsFile().toPath();
		try {
			return Files.readAllLines(outputPath);
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read lines of output from '" + outputPath + "'", ex);
		}
	}

	/**
	 * Normalizes the logging by writing the normalized lines of output to a file.
	 * @throws RuntimeException if failed to write the normalized lines of output to the
	 * file
	 */
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

	/**
	 * Normalizes a list of lines by applying a set of predefined regular expression
	 * patterns.
	 * @param lines the list of lines to be normalized
	 * @return the normalized list of lines
	 */
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

	/**
	 * Normalizes the given list of lines by replacing occurrences of a specified pattern
	 * with a replacement string.
	 * @param lines the list of lines to be normalized
	 * @param pattern the pattern to be matched for replacement
	 * @param replacement the string to replace the matched pattern
	 * @return the list of normalized lines
	 */
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

	/**
	 * Reports any unmatched normalization in the given list of lines using the specified
	 * pattern.
	 * @param lines the list of lines to check for unmatched normalization
	 * @param pattern the pattern to match against the lines
	 * @throws IllegalStateException if the pattern does not match any of the lines
	 */
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
