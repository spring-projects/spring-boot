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

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

/**
 * @author Andy Wilkinson
 */
public class ProjectCreator {

	public ProjectConnection createProject(String name) throws IOException {
		File projectDirectory = new File("target/" + name);
		projectDirectory.mkdirs();

		File gradleScript = new File(projectDirectory, "build.gradle");

		if (new File("src/test/resources", name).isDirectory()) {
			FileSystemUtils.copyRecursively(new File("src/test/resources", name),
					projectDirectory);
		}
		else {
			FileCopyUtils.copy(new File("src/test/resources/" + name + ".gradle"),
					gradleScript);
		}

		GradleConnector gradleConnector = GradleConnector.newConnector();
		((DefaultGradleConnector) gradleConnector).embedded(true);
		return gradleConnector.forProjectDirectory(projectDirectory).connect();
	}
}
