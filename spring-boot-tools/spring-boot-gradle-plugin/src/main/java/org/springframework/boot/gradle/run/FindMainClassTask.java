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

package org.springframework.boot.gradle.run;

import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Task to find and set the 'mainClassName' convention when it's missing by searching the
 * main source code.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class FindMainClassTask extends DefaultTask {

	@Input
	private SourceSetOutput mainClassSourceSetOutput;

	public void setMainClassSourceSetOutput(SourceSetOutput sourceSetOutput) {
		this.mainClassSourceSetOutput = sourceSetOutput;
		this.dependsOn(this.mainClassSourceSetOutput.getBuildDependencies());
	}

	@TaskAction
	public void setMainClassNameProperty() {
		Project project = getProject();
		if (!project.hasProperty("mainClassName")
				|| project.property("mainClassName") == null) {
			String mainClass = findMainClass();
			if (project.hasProperty("mainClassName")) {
				project.setProperty("mainClassName", mainClass);
			}
			else {
				ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) project
						.getExtensions().getByName("ext");
				extraProperties.set("mainClassName", mainClass);
			}
		}
	}

	private String findMainClass() {
		Project project = getProject();

		String mainClass = null;

		// Try the SpringBoot extension setting
		SpringBootPluginExtension bootExtension = project.getExtensions()
				.getByType(SpringBootPluginExtension.class);
		if (bootExtension.getMainClass() != null) {
			mainClass = bootExtension.getMainClass();
		}

		ApplicationPluginConvention application = (ApplicationPluginConvention) project
				.getConvention().getPlugins().get("application");

		if (mainClass == null && application != null) {
			// Try the Application extension setting
			mainClass = application.getMainClassName();
		}

		JavaExec runTask = findRunTask(project);
		if (mainClass == null && runTask != null) {
			mainClass = runTask.getMain();
		}

		if (mainClass == null) {
			Task bootRunTask = project.getTasks().findByName("bootRun");
			if (bootRunTask != null) {
				mainClass = (String) bootRunTask.property("main");
			}
		}

		if (mainClass == null) {
			// Search
			if (this.mainClassSourceSetOutput != null) {
				project.getLogger().debug("Looking for main in: "
						+ this.mainClassSourceSetOutput.getClassesDir());
				try {
					mainClass = MainClassFinder.findSingleMainClass(
							this.mainClassSourceSetOutput.getClassesDir());
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
		if (application != null && application.getMainClassName() == null) {
			application.setMainClassName(mainClass);
		}
		if (runTask != null && !runTask.hasProperty("main")) {
			runTask.setMain(mainClass);
		}

		return mainClass;
	}

	private JavaExec findRunTask(Project project) {
		Task runTask = project.getTasks().findByName("run");
		if (runTask instanceof JavaExec) {
			return (JavaExec) runTask;
		}
		return null;
	}

}
