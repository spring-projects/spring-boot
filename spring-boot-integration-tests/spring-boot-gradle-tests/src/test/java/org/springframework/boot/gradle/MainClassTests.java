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

import java.io.IOException;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for configuring a project's main class.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class MainClassTests {

	private static final String BOOT_VERSION = Versions.getBootVersion();

	private static ProjectConnection project;

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("main-class");
	}

	@Test
	public void mainFromBootRun() {
		project.newBuild().forTasks("build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-PbootRunMain=true")
				.run();
	}

	@Test
	public void nonJavaExecRunTaskIsIgnored() {
		try {
			project.newBuild().forTasks("build").withArguments(
					"-PbootVersion=" + BOOT_VERSION, "-PnonJavaExecRun=true").run();
		}
		catch (BuildException ex) {
			Throwable rootCause = getRootCause(ex);
			assertThat(rootCause.getMessage()).isEqualTo("Unable to find main class");
		}
	}

	private Throwable getRootCause(Throwable ex) {
		Throwable candidate = ex;
		while (candidate.getCause() != null) {
			candidate = candidate.getCause();
		}
		return candidate;
	}

}
