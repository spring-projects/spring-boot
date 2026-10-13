/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.test.autoconfigure;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * {@link Plugin} for projects that define test auto-configuration. When the
 * {@link JavaPlugin} is applied it:
 *
 * <ul>
 * <li>Add checks to ensure AutoConfigure*.import files and related annotations are
 * correct</li>
 * </ul>
 *
 * @author Andy Wilkinson
 */
public class TestAutoConfigurationPlugin implements Plugin<Project> {

	@Override
	public void apply(Project target) {
		target.getPlugins().withType(JavaPlugin.class, (plugin) -> {
			TaskProvider<CheckAutoConfigureImports> checkAutoConfigureImports = target.getTasks()
				.register("checkAutoConfigureImports", CheckAutoConfigureImports.class, (task) -> {
					SourceSet mainSourceSet = target.getExtensions()
						.getByType(JavaPluginExtension.class)
						.getSourceSets()
						.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
					task.setSource(mainSourceSet.getResources());
					ConfigurableFileCollection classpath = target.files(mainSourceSet.getRuntimeClasspath(),
							target.getConfigurations().getByName(mainSourceSet.getRuntimeClasspathConfigurationName()));
					task.setClasspath(classpath);
				});
			target.getTasks()
				.named(LifecycleBasePlugin.CHECK_TASK_NAME)
				.configure((check) -> check.dependsOn(checkAutoConfigureImports));
		});
	}

}
