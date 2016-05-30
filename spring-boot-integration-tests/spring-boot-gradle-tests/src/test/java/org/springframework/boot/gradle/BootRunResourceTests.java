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

import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for bootRun's resource handling
 *
 * @author Andy Wilkinson
 */
public class BootRunResourceTests {

	private static final String BOOT_VERSION = Versions.getBootVersion();

	private static ProjectConnection project;

	@Rule
	public OutputCapture output = new OutputCapture();

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("boot-run-resources");
	}

	@Test
	public void resourcesDirectlyFromSource() {
		project.newBuild().forTasks("clean", "bootRun")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-PaddResources=true")
				.run();

		assertThat(this.output.toString()).contains("src/main/resources/test.txt");
	}

	@Test
	public void resourcesFromBuildOutput() {
		project.newBuild().forTasks("clean", "bootRun")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "-PaddResources=false")
				.run();
		assertThat(this.output.toString()).contains("build/resources/main/test.txt");
	}

}
