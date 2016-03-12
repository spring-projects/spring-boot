/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.regex.Pattern;

import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for creating a fully executable jar with Gradle.
 *
 * @author Andy Wilkinson
 */
public class FullyExecutableJarTests {

	private static final String BOOT_VERSION = Versions.getBootVersion();

	private static ProjectConnection project;

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("executable-jar");
	}

	@Test
	public void jarIsNotExecutableByDefault() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION).run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(isFullyExecutable(executableJar)).isFalse();
	}

	@Test
	public void madeExecutableViaExtension() throws IOException {
		project.newBuild().forTasks("clean", "build").withArguments(
				"-PbootVersion=" + BOOT_VERSION, "-PextensionExecutable=true").run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(isFullyExecutable(executableJar)).isTrue();
	}

	@Test
	public void madeExecutableViaTask() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-PtaskExecutable=true")
				.run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(isFullyExecutable(executableJar)).isTrue();
	}

	@Test
	public void taskTakesPrecedenceForMakingJarExecutable() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION,
						"-PextensionExecutable=false", "-PtaskExecutable=true")
				.run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(isFullyExecutable(executableJar)).isTrue();
	}

	@Test
	public void scriptPropertiesFromTask() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-PtaskProperties=true")
				.run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(isFullyExecutable(executableJar)).isTrue();
		assertThat(containsLine("# Provides:.*__task__", executableJar)).isTrue();
	}

	@Test
	public void scriptPropertiesFromExtension() throws IOException {
		project.newBuild().forTasks("clean", "build").withArguments(
				"-PbootVersion=" + BOOT_VERSION, "-PextensionProperties=true").run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(isFullyExecutable(executableJar)).isTrue();
		assertThat(containsLine("# Provides:.*__extension__", executableJar)).isTrue();
	}

	@Test
	public void taskTakesPrecedenceForScriptProperties() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION,
						"-PextensionProperties=true", "-PtaskProperties=true")
				.run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(isFullyExecutable(executableJar)).isTrue();
		assertThat(containsLine("# Provides:.*__task__", executableJar)).isTrue();
	}

	@Test
	public void customScriptFromTask() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-PtaskScript=true")
				.run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(containsLine("Custom task script", executableJar)).isTrue();
	}

	@Test
	public void customScriptFromExtension() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-PextensionScript=true")
				.run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(containsLine("Custom extension script", executableJar)).isTrue();
	}

	@Test
	public void taskTakesPrecedenceForCustomScript() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-PextensionScript=true",
						"-PtaskScript=true")
				.run();
		File buildLibs = new File("target/executable-jar/build/libs");
		File executableJar = new File(buildLibs, "executable-jar.jar");
		assertThat(containsLine("Custom task script", executableJar)).isTrue();
	}

	private boolean isFullyExecutable(File file) throws IOException {
		return containsLine("#!/bin/bash", file);
	}

	private boolean containsLine(String toMatch, File file) throws IOException {
		Pattern pattern = Pattern.compile(toMatch);
		for (String line : readLines(file)) {
			if (pattern.matcher(line).matches()) {
				return true;
			}
		}
		return false;
	}

	private List<String> readLines(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		List<String> lines = new ArrayList<String>();
		try {
			String line = reader.readLine();
			while (line != null && lines.size() < 50) {
				lines.add(line);
				line = reader.readLine();
			}
		}
		finally {
			reader.close();
		}
		return lines;
	}

}
