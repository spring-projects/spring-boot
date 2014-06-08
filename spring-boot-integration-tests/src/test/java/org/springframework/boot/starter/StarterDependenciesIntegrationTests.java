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

package org.springframework.boot.starter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.boot.dependency.tools.ManagedDependencies;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.fail;

/**
 * Tests for the various starter projects to check that they don't pull in unwanted
 * transitive dependencies when used with Gradle
 * 
 * @author Andy Wilkinson
 */
@RunWith(Parameterized.class)
public class StarterDependenciesIntegrationTests {

	private static final String STARTER_NAME_PREFIX = "spring-boot-starter";

	private static final List<String> EXCLUDED_STARTERS = Arrays
			.asList("spring-boot-starter-parent");

	private static ProjectConnection project;

	private static String bootVersion;

	private static String springVersion;

	private final String[] buildArguments;

	@Parameters
	public static List<String[]> getStarters() {
		List<String[]> starters = new ArrayList<String[]>();
		for (File file : new File("../spring-boot-starters").listFiles()) {
			if (file.isDirectory()) {
				String name = file.getName();
				if (name.startsWith(STARTER_NAME_PREFIX)
						&& !EXCLUDED_STARTERS.contains(file.getName())) {
					starters.add(new String[] { file.getName() });
				}
			}
		}
		return starters;
	}

	@BeforeClass
	public static void createProject() throws IOException {
		File projectDirectory = new File("target/starter-dependencies");
		projectDirectory.mkdirs();

		File gradleScript = new File(projectDirectory, "build.gradle");
		FileCopyUtils.copy(new File("src/test/resources/build.gradle"), gradleScript);

		GradleConnector gradleConnector = GradleConnector.newConnector();
		((DefaultGradleConnector) gradleConnector).embedded(true);
		project = gradleConnector.forProjectDirectory(projectDirectory).connect();
	}

	@BeforeClass
	public static void determineVersions() throws Exception {
		springVersion = ManagedDependencies.get().find("spring-core").getVersion();
		bootVersion = ManagedDependencies.get().find("spring-boot").getVersion();
	}

	@AfterClass
	public static void closeProject() {
		project.close();
	}

	public StarterDependenciesIntegrationTests(String starter) {
		this.buildArguments = new String[] { "-Pstarter=" + starter,
				"-PbootVersion=" + bootVersion, "-PspringVersion=" + springVersion };
	}

	@Test
	public void commonsLoggingIsNotATransitiveDependency() throws IOException {
		runBuildForTask("checkCommonsLogging");
	}

	@Test
	public void oldSpringModulesAreNotTransitiveDependencies() throws IOException {
		runBuildForTask("checkSpring");
	}

	private void runBuildForTask(String task) {
		try {
			project.newBuild().forTasks(task).withArguments(this.buildArguments).run();
		}
		catch (BuildException ex) {
			Throwable root = ex;
			while (root.getCause() != null) {
				root = root.getCause();
			}
			fail(root.getMessage());
		}
	}
}
