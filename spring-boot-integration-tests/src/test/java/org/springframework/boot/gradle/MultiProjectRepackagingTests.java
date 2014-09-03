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

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.dependency.tools.ManagedDependencies;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for Gradle repackaging with a multi-project build.
 *
 * @author Andy Wilkinson
 */
public class MultiProjectRepackagingTests {

	private static final String BOOT_VERSION = ManagedDependencies.get()
			.find("spring-boot").getVersion();

	private static ProjectConnection project;

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("multi-project-repackage");
	}

	@Test
	public void repackageWithTransitiveFileDependency() throws Exception {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-Prepackage=true").run();
		File buildLibs = new File("target/multi-project-repackage/main/build/libs");
		JarFile jarFile = new JarFile(new File(buildLibs, "main.jar"));
		assertThat(jarFile.getEntry("lib/commons-logging-1.1.3.jar"), notNullValue());
		assertThat(jarFile.getEntry("lib/foo.jar"), notNullValue());
		jarFile.close();
	}
}
