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

package org.springframework.boot.maven;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Helper class to build the command-line arguments of a java process.
 *
 * @author Stephane Nicoll
 */
final class CommandLineBuilder {

	private final List<String> options = new ArrayList<>();

	private final List<URL> classpathElements = new ArrayList<>();

	private final String mainClass;

	private final List<String> arguments = new ArrayList<>();

	/**
	 * Constructs a new CommandLineBuilder with the specified main class.
	 * @param mainClass the main class to be executed
	 */
	private CommandLineBuilder(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * Creates a new instance of CommandLineBuilder for the specified main class.
	 * @param mainClass the fully qualified name of the main class
	 * @return a new instance of CommandLineBuilder
	 */
	static CommandLineBuilder forMainClass(String mainClass) {
		return new CommandLineBuilder(mainClass);
	}

	/**
	 * Adds JVM arguments to the command line builder.
	 * @param jvmArguments the JVM arguments to be added
	 * @return the updated CommandLineBuilder object
	 */
	CommandLineBuilder withJvmArguments(String... jvmArguments) {
		if (jvmArguments != null) {
			this.options.addAll(Arrays.stream(jvmArguments).filter(Objects::nonNull).toList());
		}
		return this;
	}

	/**
	 * Adds system properties to the command line builder.
	 * @param systemProperties a map of system properties to be added
	 * @return the updated command line builder
	 */
	CommandLineBuilder withSystemProperties(Map<String, String> systemProperties) {
		if (systemProperties != null) {
			systemProperties.entrySet()
				.stream()
				.map((e) -> SystemPropertyFormatter.format(e.getKey(), e.getValue()))
				.forEach(this.options::add);
		}
		return this;
	}

	/**
	 * Adds the specified elements to the classpath.
	 * @param elements the URLs representing the elements to be added to the classpath
	 * @return the CommandLineBuilder instance with the added classpath elements
	 */
	CommandLineBuilder withClasspath(URL... elements) {
		this.classpathElements.addAll(Arrays.asList(elements));
		return this;
	}

	/**
	 * Adds the specified arguments to the command line builder.
	 * @param arguments the arguments to be added to the command line builder
	 * @return the updated command line builder
	 */
	CommandLineBuilder withArguments(String... arguments) {
		if (arguments != null) {
			this.arguments.addAll(Arrays.stream(arguments).filter(Objects::nonNull).toList());
		}
		return this;
	}

	/**
	 * Builds the command line for executing a Java program.
	 * @return the command line as a list of strings
	 */
	List<String> build() {
		List<String> commandLine = new ArrayList<>();
		if (!this.options.isEmpty()) {
			commandLine.addAll(this.options);
		}
		if (!this.classpathElements.isEmpty()) {
			commandLine.add("-cp");
			commandLine.add(ClasspathBuilder.build(this.classpathElements));
		}
		commandLine.add(this.mainClass);
		if (!this.arguments.isEmpty()) {
			commandLine.addAll(this.arguments);
		}
		return commandLine;
	}

	/**
	 * ClasspathBuilder class.
	 */
	static class ClasspathBuilder {

		/**
		 * Builds a classpath string from a list of classpath elements.
		 * @param classpathElements the list of classpath elements to build the classpath
		 * from
		 * @return the classpath string
		 */
		static String build(List<URL> classpathElements) {
			StringBuilder classpath = new StringBuilder();
			for (URL element : classpathElements) {
				if (!classpath.isEmpty()) {
					classpath.append(File.pathSeparator);
				}
				classpath.append(toFile(element));
			}
			return classpath.toString();
		}

		/**
		 * Converts a URL element to a File object.
		 * @param element the URL element to be converted
		 * @return the corresponding File object
		 * @throws IllegalArgumentException if the URL element is not a valid URI
		 */
		private static File toFile(URL element) {
			try {
				return new File(element.toURI());
			}
			catch (URISyntaxException ex) {
				throw new IllegalArgumentException(ex);
			}
		}

	}

	/**
	 * Format System properties.
	 */
	private static final class SystemPropertyFormatter {

		/**
		 * Formats a system property key-value pair into a command line argument format.
		 * @param key the key of the system property
		 * @param value the value of the system property
		 * @return the formatted command line argument
		 */
		static String format(String key, String value) {
			if (key == null) {
				return "";
			}
			if (value == null || value.isEmpty()) {
				return String.format("-D%s", key);
			}
			return String.format("-D%s=\"%s\"", key, value);
		}

	}

}
