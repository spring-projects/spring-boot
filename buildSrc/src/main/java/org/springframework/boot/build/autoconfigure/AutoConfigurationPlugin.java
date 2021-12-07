/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.context.properties.ConfigurationPropertiesPlugin;

/**
 * {@link Plugin} for projects that define auto-configuration. When applied, the plugin
 * applies the {@link DeployedPlugin}. Additionally, it reacts to the presence of the
 * {@link JavaPlugin} by:
 *
 * <ul>
 * <li>Applying the {@link ConfigurationPropertiesPlugin}.
 * <li>Adding a dependency on the auto-configuration annotation processor.
 * <li>Defining a task that produces metadata describing the auto-configuration. The
 * metadata is made available as an artifact in the
 * </ul>
 *
 * @author Andy Wilkinson
 */
public class AutoConfigurationPlugin implements Plugin<Project> {

	/**
	 * Name of the {@link Configuration} that holds the auto-configuration metadata
	 * artifact.
	 */
	public static final String AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME = "autoConfigurationMetadata";

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(DeployedPlugin.class);
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> {
			project.getPlugins().apply(ConfigurationPropertiesPlugin.class);
			Configuration annotationProcessors = project.getConfigurations()
					.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
			annotationProcessors.getDependencies()
					.add(project.getDependencies().project(Collections.singletonMap("path",
							":spring-boot-project:spring-boot-tools:spring-boot-autoconfigure-processor")));
			annotationProcessors.getDependencies()
					.add(project.getDependencies().project(Collections.singletonMap("path",
							":spring-boot-project:spring-boot-tools:spring-boot-configuration-processor")));
			project.getTasks().create("autoConfigurationMetadata", AutoConfigurationMetadata.class, (task) -> {
				SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
						.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
				task.setSourceSet(main);
				task.dependsOn(main.getClassesTaskName());
				task.setOutputFile(new File(project.getBuildDir(), "auto-configuration-metadata.properties"));
				project.getArtifacts().add(AutoConfigurationPlugin.AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME,
						project.provider((Callable<File>) task::getOutputFile), (artifact) -> artifact.builtBy(task));
			});
		});
	}

}
