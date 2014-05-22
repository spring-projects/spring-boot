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

package org.springframework.boot.gradle.repackage;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.BasePlugin;
import org.springframework.boot.gradle.PluginFeatures;

/**
 * {@link PluginFeatures} to add repackage support.
 *
 * @author Phillip Webb
 */
public class RepackagePluginFeatures implements PluginFeatures {

	private static final String REPACKAGE_TASK_NAME = "bootRepackage";

	@Override
	public void apply(Project project) {
		addRepackageTask(project);
		registerRepackageTaskProperty(project);
	}

	private void addRepackageTask(Project project) {
		RepackageTask task = project.getTasks().create(REPACKAGE_TASK_NAME,
				RepackageTask.class);
		task.setDescription("Repackage existing JAR and WAR "
				+ "archives so that they can be executed from the command "
				+ "line using 'java -jar'");
		task.setGroup(BasePlugin.BUILD_GROUP);
		task.dependsOn(project.getConfigurations()
				.getByName(Dependency.ARCHIVES_CONFIGURATION).getAllArtifacts()
				.getBuildDependencies());
		ensureTaskRunsOnAssembly(project, task);
	}

	private void ensureTaskRunsOnAssembly(Project project, Task task) {
		project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(task);
	}

	/**
	 * Register BootRepackage so that we can use task {@code foo(type: BootRepackage)}.
	 */
	private void registerRepackageTaskProperty(Project project) {
		project.getExtensions().getExtraProperties()
				.set("BootRepackage", RepackageTask.class);
	}

}
