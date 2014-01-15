/*
 * Copyright 2012-2013 the original author or authors.
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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.springframework.boot.gradle.task.Repackage;
import org.springframework.boot.gradle.task.RunApp;

/**
 * Gradle 'Spring Boot' {@link Plugin}. Provides 2 tasks (bootRepackge and bootRun).
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class SpringBootPlugin implements Plugin<Project> {

	private static final String REPACKAGE_TASK_NAME = "bootRepackage";

	private static final String RUN_APP_TASK_NAME = "bootRun";

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(BasePlugin.class);
		project.getPlugins().apply(JavaPlugin.class);
		project.getPlugins().apply(ApplicationPlugin.class);
		project.getExtensions().create("springBoot", SpringBootPluginExtension.class);

		// register BootRepackage so that we can use
		// task foo(type: BootRepackage) {}
		project.getExtensions().getExtraProperties().set("BootRepackage", Repackage.class);
		Repackage packageTask = addRepackageTask(project);
		ensureTaskRunsOnAssembly(project, packageTask);
		addRunAppTask(project);
	}

	private void addRunAppTask(Project project) {
		RunApp runJarTask = project.getTasks().create(RUN_APP_TASK_NAME, RunApp.class);
		runJarTask.setDescription("Run the project with support for auto-detecting main class and reloading static resources");
		runJarTask.setGroup("Execution");
		runJarTask.dependsOn("assemble");
	}

	private Repackage addRepackageTask(Project project) {
		Repackage packageTask = project.getTasks().create(REPACKAGE_TASK_NAME,
				Repackage.class);
		packageTask.setDescription("Repackage existing JAR and WAR "
				+ "archives so that they can be executed from the command "
				+ "line using 'java -jar'");
		packageTask.setGroup(BasePlugin.BUILD_GROUP);
		packageTask.dependsOn(project.getConfigurations().getByName(
				Dependency.ARCHIVES_CONFIGURATION).getAllArtifacts().getBuildDependencies());
		return packageTask;
	}

	private void ensureTaskRunsOnAssembly(Project project, Repackage task) {
		project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(task);
	}
}
