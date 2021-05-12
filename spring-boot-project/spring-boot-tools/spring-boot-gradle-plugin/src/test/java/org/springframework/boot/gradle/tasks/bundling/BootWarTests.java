/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BootWar}.
 *
 * @author Andy Wilkinson
 */
class BootWarTests extends AbstractBootArchiveTests<BootWar> {

	BootWarTests() {
		super(BootWar.class, "org.springframework.boot.loader.WarLauncher", "WEB-INF/lib/", "WEB-INF/classes/");
	}

	@Test
	void providedClasspathJarsArePackagedInWebInfLibProvided() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		getTask().providedClasspath(jarFile("one.jar"), jarFile("two.jar"));
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/one.jar")).isNotNull();
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/two.jar")).isNotNull();
		}
	}

	@Test
	void providedClasspathCanBeSetUsingAFileCollection() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		getTask().providedClasspath(jarFile("one.jar"));
		getTask().setProvidedClasspath(getTask().getProject().files(jarFile("two.jar")));
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/one.jar")).isNull();
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/two.jar")).isNotNull();
		}
	}

	@Test
	void providedClasspathCanBeSetUsingAnObject() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		getTask().providedClasspath(jarFile("one.jar"));
		getTask().setProvidedClasspath(jarFile("two.jar"));
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/one.jar")).isNull();
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/two.jar")).isNotNull();
		}
	}

	@Test
	void devtoolsJarIsExcludedByDefaultWhenItsOnTheProvidedClasspath() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		getTask().providedClasspath(newFile("spring-boot-devtools-0.1.2.jar"));
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/spring-boot-devtools-0.1.2.jar")).isNull();
		}
	}

	@Test
	@Deprecated
	void devtoolsJarCanBeIncludedWhenItsOnTheProvidedClasspath() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		getTask().providedClasspath(jarFile("spring-boot-devtools-0.1.2.jar"));
		getTask().setExcludeDevtools(false);
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/spring-boot-devtools-0.1.2.jar")).isNotNull();
		}
	}

	@Test
	void webappResourcesInDirectoriesThatOverlapWithLoaderCanBePackaged() throws IOException {
		File webappDirectory = new File(this.temp, "src/main/webapp");
		webappDirectory.mkdirs();
		File orgDirectory = new File(webappDirectory, "org");
		orgDirectory.mkdir();
		new File(orgDirectory, "foo.txt").createNewFile();
		getTask().from(webappDirectory);
		getTask().getMainClass().set("com.example.Main");
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("org/")).isNotNull();
			assertThat(jarFile.getEntry("org/foo.txt")).isNotNull();
		}
	}

	@Test
	void libProvidedEntriesAreWrittenAfterLibEntries() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		getTask().classpath(jarFile("library.jar"));
		getTask().providedClasspath(jarFile("provided-library.jar"));
		executeTask();
		assertThat(getEntryNames(getTask().getArchiveFile().get().getAsFile()))
				.containsSubsequence("WEB-INF/lib/library.jar", "WEB-INF/lib-provided/provided-library.jar");
	}

	@Override
	protected void executeTask() {
		getTask().copy();
	}

}
