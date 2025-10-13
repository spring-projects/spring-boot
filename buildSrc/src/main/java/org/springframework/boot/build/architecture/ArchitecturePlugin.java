/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.architecture;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.springframework.util.StringUtils;

/**
 * {@link Plugin} for verifying a project's architecture.
 *
 * @author Andy Wilkinson
 */
public class ArchitecturePlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		ArchitectureCheckExtension extension = project.getExtensions()
			.create("architectureCheck", ArchitectureCheckExtension.class);
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> registerTasks(project, extension));
	}

	private void registerTasks(Project project, ArchitectureCheckExtension extension) {
		JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		List<TaskProvider<ArchitectureCheck>> packageTangleChecks = new ArrayList<>();
		for (SourceSet sourceSet : javaPluginExtension.getSourceSets()) {
			TaskProvider<ArchitectureCheck> checkPackageTangles = project.getTasks()
				.register("checkArchitecture" + StringUtils.capitalize(sourceSet.getName()), ArchitectureCheck.class,
						(task) -> {
							task.getSourceSet().set(sourceSet.getName());
							task.getCompileClasspath().from(sourceSet.getCompileClasspath());
							task.setClasses(sourceSet.getOutput().getClassesDirs());
							task.getResourcesDirectory().set(sourceSet.getOutput().getResourcesDir());
							task.dependsOn(sourceSet.getProcessResourcesTaskName());
							task.setDescription("Checks the architecture of the classes of the " + sourceSet.getName()
									+ " source set.");
							task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
							task.getNullMarkedEnabled().set(extension.getNullMarked().getEnabled());
							task.getNullMarkedIgnoredPackages().set(extension.getNullMarked().getIgnoredPackages());
						});
			packageTangleChecks.add(checkPackageTangles);
		}
		if (!packageTangleChecks.isEmpty()) {
			TaskProvider<Task> checkTask = project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME);
			checkTask.configure((check) -> check.dependsOn(packageTangleChecks));
		}
	}

}
