/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool;

import org.springframework.util.StringUtils;

/**
 * {@link Plugin} for verifying a project's architecture.
 *
 * @author Andy Wilkinson
 */
public class ArchitecturePlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> registerTasks(project));
	}

	private void registerTasks(Project project) {
		JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		for (SourceSet sourceSet : javaPluginExtension.getSourceSets()) {
			registerArchitectureCheck(sourceSet, "java", project)
				.configure((task) -> task.setClasses(project.files(project.getTasks()
					.named(sourceSet.getCompileTaskName("java"), JavaCompile.class)
					.flatMap((compile) -> compile.getDestinationDirectory()))));
			project.getPlugins()
				.withId("org.jetbrains.kotlin.jvm",
						(kotlinPlugin) -> registerArchitectureCheck(sourceSet, "kotlin", project)
							.configure((task) -> task.setClasses(project.files(project.getTasks()
								.named(sourceSet.getCompileTaskName("kotlin"), KotlinCompileTool.class)
								.flatMap((compile) -> compile.getDestinationDirectory())))));
		}
	}

	private TaskProvider<ArchitectureCheck> registerArchitectureCheck(SourceSet sourceSet, String language,
			Project project) {
		TaskProvider<ArchitectureCheck> checkArchitecture = project.getTasks()
			.register(
					"checkArchitecture"
							+ StringUtils.capitalize(sourceSet.getName() + StringUtils.capitalize(language)),
					ArchitectureCheck.class, (task) -> {
						task.getSourceSet().set(sourceSet.getName());
						task.getCompileClasspath().from(sourceSet.getCompileClasspath());
						task.getResourcesDirectory().set(sourceSet.getOutput().getResourcesDir());
						task.dependsOn(sourceSet.getProcessResourcesTaskName());
						task.setDescription("Checks the architecture of the " + language + " classes of the "
								+ sourceSet.getName() + " source set.");
						task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
					});
		project.getTasks()
			.named(LifecycleBasePlugin.CHECK_TASK_NAME)
			.configure((check) -> check.dependsOn(checkArchitecture));
		return checkArchitecture;
	}

}
