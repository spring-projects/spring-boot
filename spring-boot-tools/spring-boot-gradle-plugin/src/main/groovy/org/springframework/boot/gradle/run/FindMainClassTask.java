/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.run;

import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Task to find and set the 'mainClassName' convention when it's missing by searching the
 * main source code.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class FindMainClassTask extends DefaultTask {

	@TaskAction
	public void setMainClassNameProperty() {
		Project project = getProject();
		if (project.property("mainClassName") == null) {
			project.setProperty("mainClassName", findMainClass());
		}
	}

	private String findMainClass() {
		Project project = getProject();

		String mainClass = null;

		// Try the SpringBoot extension setting
		SpringBootPluginExtension bootExtension = project.getExtensions().getByType(
				SpringBootPluginExtension.class);
		if (bootExtension.getMainClass() != null) {
			mainClass = bootExtension.getMainClass();
		}

		ApplicationPluginConvention application = (ApplicationPluginConvention) project
				.getConvention().getPlugins().get("application");
		// Try the Application extension setting
		if (mainClass == null && application.getMainClassName() != null) {
			mainClass = application.getMainClassName();
		}

		Task runTask = getProject().getTasks().getByName("run");
		if (mainClass == null && runTask.hasProperty("main")) {
			mainClass = (String) runTask.property("main");
		}

		if (mainClass == null) {
			// Search
			SourceSet mainSourceSet = SourceSets.findMainSourceSet(project);
			if (mainSourceSet != null) {
				project.getLogger().debug(
						"Looking for main in: "
								+ mainSourceSet.getOutput().getClassesDir());
				try {
					mainClass = MainClassFinder.findSingleMainClass(mainSourceSet
							.getOutput().getClassesDir());
					project.getLogger().info("Computed main class: " + mainClass);
				}
				catch (IOException ex) {
					throw new IllegalStateException("Cannot find main class", ex);
				}
			}
		}

		project.getLogger().info("Found main: " + mainClass);

		if (bootExtension.getMainClass() == null) {
			bootExtension.setMainClass(mainClass);
		}
		if (application.getMainClassName() == null) {
			application.setMainClassName(mainClass);
		}
		if (!runTask.hasProperty("main")) {
			runTask.setProperty("main", mainClass);
		}

		return mainClass;
	}
}
