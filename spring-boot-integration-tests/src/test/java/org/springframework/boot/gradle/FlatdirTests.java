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

import org.gradle.tooling.ProjectConnection;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.dependency.tools.ManagedDependencies;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

/**
 * Tests for using the Gradle plugin's support for flat directory repos
 *
 * @author Dave Syer
 */
public class FlatdirTests {

	private ProjectConnection project;

	private File libs = new File("target/flatdir/lib");

	private static final String BOOT_VERSION = ManagedDependencies.get().find(
			"spring-boot").getVersion();
	
	@Before
	public void init() {
		if (libs.exists()) {
			FileSystemUtils.deleteRecursively(libs);
		}
	}

	@Test
	public void flatdir() throws Exception {
		project = new ProjectCreator().createProject("flatdir");
		if (!libs.exists()) {
			libs.mkdirs();
		}
		FileCopyUtils.copy(new File("src/test/resources/foo.jar"),
				new File(libs, "foo-1.0.0.jar"));
		project.newBuild().forTasks("build").withArguments(
				"-PbootVersion=" + BOOT_VERSION, "--stacktrace").run();
	}

}
