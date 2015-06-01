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

import org.gradle.tooling.ProjectConnection;
import org.junit.Test;
import org.springframework.boot.dependency.tools.ManagedDependencies;

/**
 * Tests for using the Gradle plugin's support for installing artifacts
 *
 * @author Dave Syer
 */
public class InstallTests {

	private ProjectConnection project;

	private static final String BOOT_VERSION = ManagedDependencies.get()
			.find("spring-boot").getVersion();

	@Test
	public void cleanInstall() throws Exception {
		project = new ProjectCreator().createProject("installer");
		project.newBuild().forTasks("install")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "--stacktrace").run();
	}

	@Test
	public void cleanInstallVersionManagement() throws Exception {
		project = new ProjectCreator().createProject("installer-io");
		project.newBuild().forTasks("install")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "--stacktrace").run();
	}

	@Test
	public void cleanInstallApp() throws Exception {
		project = new ProjectCreator().createProject("install-app");
		// "install" from the application plugin was renamed "installApp" in Gradle
		// 1.0
		project.newBuild().forTasks("installApp")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "--stacktrace", "--info")
				.run();
	}

}
