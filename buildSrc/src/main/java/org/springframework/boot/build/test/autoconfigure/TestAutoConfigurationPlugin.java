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
