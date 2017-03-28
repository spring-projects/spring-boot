/*
 * Copyright 2012-2017 the original author or authors.
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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

import org.springframework.boot.gradle.buildinfo.BuildInfo;

/**
 * Gradle DSL extension that provides the entry point to Spring Boot's DSL.
 *
 * @author Andy Wilkinson
 */
public class SpringBootExtension {

	private final Project project;

	/**
	 * Creates a new {@code SpringBootPluginExtension} that is associated with the given
	 * {@code project}.
	 *
	 * @param project the project
	 */
	public SpringBootExtension(Project project) {
		this.project = project;
	}

	/**
	 * Creates a new {@link BuildInfo} task named {@code bootBuildInfo} and configures the
	 * {@code classes} task to depend upon it.
	 */
	public void buildInfo() {
		this.buildInfo(null);
	}

	/**
	 * Creates a new {@link BuildInfo} task named {@code bootBuildInfo} and configures the
	 * {@code classes} task to depend upon it. The task is passed to the given
	 * {@code configurer} for further configuration.
	 *
	 * @param configurer the task configurer
	 */
	public void buildInfo(Action<BuildInfo> configurer) {
		BuildInfo bootBuildInfo = this.project.getTasks().create("bootBuildInfo",
				BuildInfo.class);
		this.project.getTasks().getByName(JavaPlugin.CLASSES_TASK_NAME)
				.dependsOn(bootBuildInfo);
		if (configurer != null) {
			configurer.execute(bootBuildInfo);
		}
	}

}
