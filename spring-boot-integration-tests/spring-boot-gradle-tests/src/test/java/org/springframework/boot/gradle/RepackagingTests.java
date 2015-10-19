/*
 * Copyright 2012-2015 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.util.FileCopyUtils;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for gradle repackaging.
 *
 * @author Andy Wilkinson
 */
public class RepackagingTests {

	private static final String BOOT_VERSION = Versions.getBootVersion();

	private static ProjectConnection project;

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("repackage");
	}

	@Test
	public void repackagingEnabled() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=true",
						"-PexcludeDevtools=false")
				.run();
		File buildLibs = new File("target/repackage/build/libs");
		File repackageFile = new File(buildLibs, "repackage.jar");
		assertTrue(repackageFile.exists());
		assertTrue(new File(buildLibs, "repackage.jar.original").exists());
		assertFalse(new File(buildLibs, "repackage-sources.jar.original").exists());
		assertTrue(isDevToolsJarIncluded(repackageFile));
	}

	@Test
	public void repackagingDisabled() {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=false",
						"-PexcludeDevtools=false")
				.run();
		File buildLibs = new File("target/repackage/build/libs");
		assertTrue(new File(buildLibs, "repackage.jar").exists());
		assertFalse(new File(buildLibs, "repackage.jar.original").exists());
		assertFalse(new File(buildLibs, "repackage-sources.jar.original").exists());
	}

	@Test
	public void repackagingDisabledWithCustomRepackagedJar() {
		project.newBuild().forTasks("clean", "build", "customRepackagedJar")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=false",
						"-PexcludeDevtools=false")
				.run();
		File buildLibs = new File("target/repackage/build/libs");
		assertTrue(new File(buildLibs, "repackage.jar").exists());
		assertFalse(new File(buildLibs, "repackage.jar.original").exists());
		assertFalse(new File(buildLibs, "repackage-sources.jar.original").exists());
		assertTrue(new File(buildLibs, "custom.jar").exists());
		assertTrue(new File(buildLibs, "custom.jar.original").exists());
	}

	@Test
	public void repackagingDisabledWithCustomRepackagedJarUsingStringJarTaskReference() {
		project.newBuild()
				.forTasks("clean", "build", "customRepackagedJarWithStringReference")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=false",
						"-PexcludeDevtools=false")
				.run();
		File buildLibs = new File("target/repackage/build/libs");
		assertTrue(new File(buildLibs, "repackage.jar").exists());
		assertFalse(new File(buildLibs, "repackage.jar.original").exists());
		assertFalse(new File(buildLibs, "repackage-sources.jar.original").exists());
		assertTrue(new File(buildLibs, "custom.jar").exists());
		assertTrue(new File(buildLibs, "custom.jar.original").exists());
	}

	@Test
	public void repackagingEnabledWithCustomRepackagedJar() {
		project.newBuild().forTasks("clean", "build", "customRepackagedJar")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=true",
						"-PexcludeDevtools=false")
				.run();
		File buildLibs = new File("target/repackage/build/libs");
		assertTrue(new File(buildLibs, "repackage.jar").exists());
		assertTrue(new File(buildLibs, "repackage.jar.original").exists());
		assertFalse(new File(buildLibs, "repackage-sources.jar.original").exists());
		assertTrue(new File(buildLibs, "custom.jar").exists());
		assertTrue(new File(buildLibs, "custom.jar.original").exists());
	}

	@Test
	public void repackagingEnableWithCustomRepackagedJarUsingStringJarTaskReference() {
		project.newBuild()
				.forTasks("clean", "build", "customRepackagedJarWithStringReference")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=true",
						"-PexcludeDevtools=false")
				.run();
		File buildLibs = new File("target/repackage/build/libs");
		assertTrue(new File(buildLibs, "repackage.jar").exists());
		assertTrue(new File(buildLibs, "repackage.jar.original").exists());
		assertFalse(new File(buildLibs, "repackage-sources.jar.original").exists());
		assertTrue(new File(buildLibs, "custom.jar").exists());
		assertTrue(new File(buildLibs, "custom.jar.original").exists());
	}

	@Test
	public void repackageWithFileDependency() throws Exception {
		FileCopyUtils.copy(new File("src/test/resources/foo.jar"),
				new File("target/repackage/foo.jar"));
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=true",
						"-PexcludeDevtools=false")
				.run();
		File buildLibs = new File("target/repackage/build/libs");
		JarFile jarFile = new JarFile(new File(buildLibs, "repackage.jar"));
		assertThat(jarFile.getEntry("lib/foo.jar"), notNullValue());
		jarFile.close();
	}

	@Test
	public void repackagingEnabledExcludeDevtools() throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=true",
						"-PexcludeDevtools=true")
				.run();
		File buildLibs = new File("target/repackage/build/libs");
		File repackageFile = new File(buildLibs, "repackage.jar");
		assertTrue(repackageFile.exists());
		assertTrue(new File(buildLibs, "repackage.jar.original").exists());
		assertFalse(new File(buildLibs, "repackage-sources.jar.original").exists());
		assertFalse(isDevToolsJarIncluded(repackageFile));
	}

	private boolean isDevToolsJarIncluded(File repackageFile) throws IOException {
		JarFile jarFile = new JarFile(repackageFile);
		try {
			String name = "lib/spring-boot-devtools-" + BOOT_VERSION + ".jar";
			return jarFile.getEntry(name) != null;
		}
		finally {
			jarFile.close();
		}
	}

}
