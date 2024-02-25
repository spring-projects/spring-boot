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

package org.springframework.boot.test.context;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.test.context.aot.TestAotProcessor;
import org.springframework.util.Assert;

/**
 * Entry point for AOT processing of a Spring Boot application's tests.
 *
 * <strong>For internal use only.</strong>
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
public class SpringBootTestAotProcessor extends TestAotProcessor {

	/**
	 * Create a new processor for the specified test classpath roots and general settings.
	 * @param classpathRoots the classpath roots to scan for test classes
	 * @param settings the general AOT processor settings
	 */
	public SpringBootTestAotProcessor(Set<Path> classpathRoots, Settings settings) {
		super(classpathRoots, settings);
	}

	/**
	 * The main method of the SpringBootTestAotProcessor class.
	 *
	 * This method is the entry point of the application. It expects 6 command line
	 * arguments: - classpathRoots: a string representing the classpath roots separated by
	 * the system's file separator - sourceOutput: the path where the source files will be
	 * generated - resourceOutput: the path where the resource files will be generated -
	 * classOutput: the path where the class files will be generated - groupId: the group
	 * ID of the project - artifactId: the artifact ID of the project
	 *
	 * If the number of arguments is less than 6, an exception will be thrown with a usage
	 * message.
	 *
	 * The method collects the classpath roots into a set of Path objects, using the
	 * system's file separator. It then creates a Settings object with the provided paths
	 * and project information. Finally, it creates a new instance of the
	 * SpringBootTestAotProcessor class and calls the process method.
	 */
	public static void main(String[] args) {
		int requiredArgs = 6;
		Assert.isTrue(args.length >= requiredArgs,
				() -> "Usage: %s <classpathRoots> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId>"
					.formatted(TestAotProcessor.class.getName()));
		Set<Path> classpathRoots = Arrays.stream(args[0].split(File.pathSeparator))
			.map(Paths::get)
			.collect(Collectors.toSet());
		Settings settings = Settings.builder()
			.sourceOutput(Paths.get(args[1]))
			.resourceOutput(Paths.get(args[2]))
			.classOutput(Paths.get(args[3]))
			.groupId(args[4])
			.artifactId(args[5])
			.build();
		new SpringBootTestAotProcessor(classpathRoots, settings).process();
	}

}
