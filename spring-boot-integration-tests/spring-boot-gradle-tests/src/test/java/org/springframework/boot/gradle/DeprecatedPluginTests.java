/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.gradle;

import org.gradle.tooling.ProjectConnection;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

/**
 * Tests for using the old, deprecated plugin ID.
 *
 * @author Andy Wilkinson
 */
public class DeprecatedPluginTests {

	private ProjectConnection project;

	private static final String BOOT_VERSION = Versions.getBootVersion();

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void deprecatedIdWorks() throws Exception {
		this.project = new ProjectCreator().createProject("deprecated-plugin");
		this.project.newBuild().forTasks("build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "--stacktrace").run();
	}

}
