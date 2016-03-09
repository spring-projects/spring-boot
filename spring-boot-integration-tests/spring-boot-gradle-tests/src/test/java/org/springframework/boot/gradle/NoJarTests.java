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

import java.io.File;

import org.gradle.tooling.ProjectConnection;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for using the Gradle plugin's support for flat directory repos
 *
 * @author Dave Syer
 */
public class NoJarTests {

	private ProjectConnection project;

	private static final String BOOT_VERSION = Versions.getBootVersion();

	@Test
	public void nojar() throws Exception {
		this.project = new ProjectCreator().createProject("nojar");
		this.project.newBuild().forTasks("build")
				.withArguments("-PbootVersion=" + BOOT_VERSION, "--stacktrace").run();
		assertThat(new File("target/nojar/build/libs")).doesNotExist();
	}

}
