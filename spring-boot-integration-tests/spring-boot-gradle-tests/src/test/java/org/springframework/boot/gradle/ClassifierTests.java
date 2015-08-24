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

import java.util.jar.JarFile;

import org.gradle.tooling.ProjectConnection;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for using the Gradle plugin's support for installing artifacts
 *
 * @author Dave Syer
 */
public class ClassifierTests {

	private ProjectConnection project;

	private static final String BOOT_VERSION = Versions.getBootVersion();

	@Test
	public void classifierInBootTask() throws Exception {
		this.project = new ProjectCreator().createProject("classifier");
		this.project.newBuild().forTasks("build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "--stacktrace").run();
		checkFilesExist("classifier");
	}

	@Test
	public void classifierInBootExtension() throws Exception {
		this.project = new ProjectCreator().createProject("classifier-extension");
		this.project.newBuild().forTasks("build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "--stacktrace", "--info")
				.run();
	}

	private void checkFilesExist(String name) throws Exception {
		JarFile jar = new JarFile("target/" + name + "/build/libs/" + name + ".jar");
		assertNotNull(jar.getManifest());
		jar.close();
		jar = new JarFile("target/" + name + "/build/libs/" + name + "-exec.jar");
		assertNotNull(jar.getManifest());
		jar.close();
	}

}
