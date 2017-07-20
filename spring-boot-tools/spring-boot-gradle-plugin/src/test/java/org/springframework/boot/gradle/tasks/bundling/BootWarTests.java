/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BootWar}.
 *
 * @author Andy Wilkinson
 */
public class BootWarTests extends AbstractBootArchiveTests<BootWar> {

	public BootWarTests() {
		super(BootWar.class, "org.springframework.boot.loader.WarLauncher", "WEB-INF/lib",
				"WEB-INF/classes");
	}

	@Test
	public void providedClasspathJarsArePackagedInWebInfLibProvided() throws IOException {
		getTask().setMainClass("com.example.Main");
		getTask().providedClasspath(this.temp.newFile("one.jar"),
				this.temp.newFile("two.jar"));
		getTask().execute();
		try (JarFile jarFile = new JarFile(getTask().getArchivePath())) {
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/one.jar")).isNotNull();
			assertThat(jarFile.getEntry("WEB-INF/lib-provided/two.jar")).isNotNull();
		}
	}

	@Test
	public void devtoolsJarIsExcludedByDefaultWhenItsOnTheProvidedClasspath()
			throws IOException {
		getTask().setMainClass("com.example.Main");
		getTask().providedClasspath(this.temp.newFile("spring-boot-devtools-0.1.2.jar"));
		getTask().execute();
		assertThat(getTask().getArchivePath().exists());
		try (JarFile jarFile = new JarFile(getTask().getArchivePath())) {
			assertThat(jarFile
					.getEntry("WEB-INF/lib-provided/spring-boot-devtools-0.1.2.jar"))
							.isNull();
		}
	}

	@Test
	public void devtoolsJarCanBeIncludedWhenItsOnTheProvidedClasspath()
			throws IOException {
		getTask().setMainClass("com.example.Main");
		getTask().providedClasspath(this.temp.newFile("spring-boot-devtools-0.1.2.jar"));
		getTask().setExcludeDevtools(false);
		getTask().execute();
		assertThat(getTask().getArchivePath().exists());
		try (JarFile jarFile = new JarFile(getTask().getArchivePath())) {
			assertThat(jarFile
					.getEntry("WEB-INF/lib-provided/spring-boot-devtools-0.1.2.jar"))
							.isNotNull();
		}
	}

	@Test
	public void webappResourcesInDirectoriesThatOverlapWithLoaderCanBePackaged()
			throws IOException {
		File webappFolder = this.temp.newFolder("src", "main", "webapp");
		File orgFolder = new File(webappFolder, "org");
		orgFolder.mkdir();
		new File(orgFolder, "foo.txt").createNewFile();
		getTask().from(webappFolder);
		getTask().setMainClass("com.example.Main");
		getTask().execute();
		assertThat(getTask().getArchivePath().exists());
		try (JarFile jarFile = new JarFile(getTask().getArchivePath())) {
			assertThat(jarFile.getEntry("org/")).isNotNull();
			assertThat(jarFile.getEntry("org/foo.txt")).isNotNull();
		}
	}

}
