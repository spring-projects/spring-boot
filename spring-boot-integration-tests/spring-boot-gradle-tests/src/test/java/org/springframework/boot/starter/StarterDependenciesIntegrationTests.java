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

package org.springframework.boot.starter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.ProjectConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.boot.gradle.ProjectCreator;
import org.springframework.boot.gradle.Versions;

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
		for (File file : new File("../../spring-boot-starters").listFiles()) {
			if (file.isDirectory() && new File(file, "pom.xml").exists()) {
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
		project = new ProjectCreator().createProject("starter-dependencies");
	}

	@BeforeClass
	public static void determineVersions() throws Exception {
		springVersion = Versions.getSpringVersion();
		bootVersion = Versions.getBootVersion();
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
