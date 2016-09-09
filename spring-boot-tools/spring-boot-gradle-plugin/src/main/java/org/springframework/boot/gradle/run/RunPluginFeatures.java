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

package org.springframework.boot.gradle.run;

import java.util.Collections;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.application.CreateStartScripts;

import org.springframework.boot.gradle.PluginFeatures;

/**
 * {@link PluginFeatures} to add run support.
 *
 * @author Phillip Webb
 */
public class RunPluginFeatures implements PluginFeatures {

	private static final String FIND_MAIN_CLASS_TASK_NAME = "findMainClass";

	private static final String RUN_APP_TASK_NAME = "bootRun";

	@Override
	public void apply(Project project) {
		mainClassNameFinder(project);
		addBootRunTask(project);
	}

	private void mainClassNameFinder(Project project) {
		FindMainClassTask findMainClassTask = project.getTasks()
				.create(FIND_MAIN_CLASS_TASK_NAME, FindMainClassTask.class);
		SourceSet mainSourceSet = SourceSets.findMainSourceSet(project);
		if (mainSourceSet != null) {
			findMainClassTask.setMainClassSourceSetOutput(mainSourceSet.getOutput());
		}
		project.getTasks().all(new Action<Task>() {
			@Override
			public void execute(Task task) {
				if (task instanceof BootRunTask || task instanceof CreateStartScripts) {
					task.dependsOn(FIND_MAIN_CLASS_TASK_NAME);
				}
			}
		});
	}

	private void addBootRunTask(final Project project) {
		final JavaPluginConvention javaConvention = project.getConvention()
				.getPlugin(JavaPluginConvention.class);

		BootRunTask run = project.getTasks().create(RUN_APP_TASK_NAME, BootRunTask.class);
		run.setDescription("Run the project with support for "
				+ "auto-detecting main class and reloading static resources");
		run.setGroup("application");
		run.setClasspath(
				javaConvention.getSourceSets().findByName("main").getRuntimeClasspath());
		run.getConventionMapping().map("main", new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				if (project.hasProperty("mainClassName")
						&& project.property("mainClassName") != null) {
					return project.property("mainClassName");
				}
				ExtraPropertiesExtension extraPropertiesExtension = (ExtraPropertiesExtension) project
						.getExtensions().getByName("ext");
				if (extraPropertiesExtension.has("mainClassName")
						&& extraPropertiesExtension.get("mainClassName") != null) {
					return extraPropertiesExtension.get("mainClassName");
				}
				return null;
			}
		});
		run.getConventionMapping().map("jvmArgs", new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				if (project.hasProperty("applicationDefaultJvmArgs")) {
					return project.property("applicationDefaultJvmArgs");
				}
				return Collections.emptyList();
			}
		});
	}
}
