/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gradle.tooling.ProjectConnection;
import org.junit.Test;
import org.springframework.boot.dependency.tools.ManagedDependencies;

import static org.junit.Assert.fail;

/**
 * Integration tests for the Gradle plugin's Spring Loaded support
 *
 * @author Andy Wilkinson
 */
public class SpringLoadedTests {

	private static final String BOOT_VERSION = ManagedDependencies.get()
			.find("spring-boot").getVersion();

	private static final String SPRING_LOADED_VERSION = ManagedDependencies.get()
			.find("springloaded").getVersion();

	@Test
	public void defaultJvmArgsArePreservedWhenLoadedAgentIsConfigured()
			throws IOException {
		ProjectConnection project = new ProjectCreator()
				.createProject("spring-loaded-jvm-args");
		project.newBuild()
				.forTasks("bootRun")
				.withArguments("-PbootVersion=" + BOOT_VERSION,
						"-PspringLoadedVersion=" + SPRING_LOADED_VERSION, "--stacktrace")
				.run();

		List<String> output = getOutput();
		assertOutputContains("-DSOME_ARG=someValue", output);
		assertOutputContains("-Xverify:none", output);
		assertOutputMatches(
				"-javaagent:.*springloaded-" + SPRING_LOADED_VERSION + ".jar", output);
	}

	@Test
	public void springLoadedCanBeUsedWithGradle16() throws IOException {
		ProjectConnection project = new ProjectCreator("1.6")
				.createProject("spring-loaded-old-gradle");
		project.newBuild()
				.forTasks("bootRun")
				.withArguments("-PbootVersion=" + BOOT_VERSION,
						"-PspringLoadedVersion=" + SPRING_LOADED_VERSION, "--stacktrace")
				.run();

		List<String> output = getOutput();
		assertOutputMatches(
				"-javaagent:.*springloaded-" + SPRING_LOADED_VERSION + ".jar", output);
	}

	private List<String> getOutput() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(
				"target/spring-loaded-jvm-args/build/output.txt")));
		try {
			List<String> lines = new ArrayList<String>();

			String line;

			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			return lines;
		}
		finally {
			reader.close();
		}
	}

	private void assertOutputContains(String requiredOutput, List<String> actualOutput) {
		for (String line : actualOutput) {
			if (line.equals(requiredOutput)) {
				return;
			}
		}
		fail("Required output '" + requiredOutput + "' not found in " + actualOutput);
	}

	private void assertOutputMatches(String requiredPattern, List<String> actualOutput) {
		for (String line : actualOutput) {
			if (line.matches(requiredPattern)) {
				return;
			}
		}
		fail("Required pattern '" + requiredPattern + "' not matched in " + actualOutput);
	}
}
