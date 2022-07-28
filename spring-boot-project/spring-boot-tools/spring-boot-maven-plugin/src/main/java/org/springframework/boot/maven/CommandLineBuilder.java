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

	private CommandLineBuilder(String mainClass) {
		this.mainClass = mainClass;
	}

	static CommandLineBuilder forMainClass(String mainClass) {
		return new CommandLineBuilder(mainClass);
	}

	CommandLineBuilder withJvmArguments(String... jvmArguments) {
		if (jvmArguments != null) {
			this.options.addAll(Arrays.stream(jvmArguments).filter(Objects::nonNull).toList());
		}
		return this;
	}

	CommandLineBuilder withSystemProperties(Map<String, String> systemProperties) {
		if (systemProperties != null) {
			systemProperties.entrySet().stream().map((e) -> SystemPropertyFormatter.format(e.getKey(), e.getValue()))
					.forEach(this.options::add);
		}
		return this;
	}

	CommandLineBuilder withClasspath(URL... elements) {
		this.classpathElements.addAll(Arrays.asList(elements));
		return this;
	}

	CommandLineBuilder withArguments(String... arguments) {
		if (arguments != null) {
			this.arguments.addAll(Arrays.stream(arguments).filter(Objects::nonNull).toList());
		}
		return this;
	}

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

	static class ClasspathBuilder {

		static String build(List<URL> classpathElements) {
			StringBuilder classpath = new StringBuilder();
			for (URL element : classpathElements) {
				if (classpath.length() > 0) {
					classpath.append(File.pathSeparator);
				}
				classpath.append(toFile(element));
			}
			return classpath.toString();
		}

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
	private static class SystemPropertyFormatter {

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
