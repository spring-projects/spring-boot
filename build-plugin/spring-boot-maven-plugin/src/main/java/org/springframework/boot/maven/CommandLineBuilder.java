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

package org.springframework.boot.maven;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

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

	// Do not use String @Nullable ... jvmArguments, Maven can't deal with that
	CommandLineBuilder withJvmArguments(@Nullable String... jvmArguments) {
		if (jvmArguments != null) {
			this.options.addAll(Arrays.stream(jvmArguments).filter(Objects::nonNull).toList());
		}
		return this;
	}

	CommandLineBuilder withSystemProperties(@Nullable Map<String, String> systemProperties) {
		if (systemProperties != null) {
			for (Entry<String, String> systemProperty : systemProperties.entrySet()) {
				String option = SystemPropertyFormatter.format(systemProperty.getKey(), systemProperty.getValue());
				if (StringUtils.hasText(option)) {
					this.options.add(option);
				}
			}
		}
		return this;
	}

	CommandLineBuilder withClasspath(URL... elements) {
		this.classpathElements.addAll(Arrays.asList(elements));
		return this;
	}

	// Do not use String @Nullable ... arguments, Maven can't deal with that
	CommandLineBuilder withArguments(@Nullable String... arguments) {
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
		commandLine.addAll(ClassPath.of(this.classpathElements).args(true));
		commandLine.add(this.mainClass);
		if (!this.arguments.isEmpty()) {
			commandLine.addAll(this.arguments);
		}
		return commandLine;
	}

}
