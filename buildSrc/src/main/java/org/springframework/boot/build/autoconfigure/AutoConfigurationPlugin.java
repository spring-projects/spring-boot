/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.architecture.ArchitecturePlugin;
import org.springframework.boot.build.optional.OptionalDependenciesPlugin;

/**
 * {@link Plugin} for projects that define auto-configuration. When applied, the plugin
 * applies the {@link DeployedPlugin}. Additionally, when the {@link JavaPlugin} is
 * applied it:
 *
 * <ul>
 * <li>Adds a dependency on the auto-configuration annotation processor.
 * <li>Defines a task that produces metadata describing the auto-configuration. The
 * metadata is made available as an artifact in the {@code autoConfigurationMetadata}
 * configuration.
 * <li>Reacts to the {@link ArchitecturePlugin} being applied and:
 * <ul>
 * <li>Adds a rule to the {@code checkArchitectureMain} task to verify that all
 * {@code AutoConfiguration} classes are listed in the {@code AutoConfiguration.imports}
 * file.
 * </ul>
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
			Configuration annotationProcessors = project.getConfigurations()
				.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
			SourceSet main = project.getExtensions()
				.getByType(JavaPluginExtension.class)
				.getSourceSets()
				.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			annotationProcessors.getDependencies()
				.add(project.getDependencies()
					.project(Collections.singletonMap("path",
							":spring-boot-project:spring-boot-tools:spring-boot-autoconfigure-processor")));
			annotationProcessors.getDependencies()
				.add(project.getDependencies()
					.project(Collections.singletonMap("path",
							":spring-boot-project:spring-boot-tools:spring-boot-configuration-processor")));
			project.getTasks().register("autoConfigurationMetadata", AutoConfigurationMetadata.class, (task) -> {
				task.setSourceSet(main);
				task.dependsOn(main.getClassesTaskName());
				task.getOutputFile()
					.set(project.getLayout().getBuildDirectory().file("auto-configuration-metadata.properties"));
				project.getArtifacts()
					.add(AutoConfigurationPlugin.AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME, task.getOutputFile(),
							(artifact) -> artifact.builtBy(task));
			});
			project.getTasks()
				.register("checkAutoConfigurationImports", CheckAutoConfigurationImports.class, (task) -> {
					task.setSource(main.getResources());
					task.setClasspath(main.getOutput().getClassesDirs());
					task.setDescription("Checks the %s file of the main source set."
						.formatted(AutoConfigurationImportsTask.IMPORTS_FILE));
				});
			Configuration requiredClasspath = project.getConfigurations()
				.create("autoConfigurationRequiredClasspath")
				.extendsFrom(project.getConfigurations().getByName(main.getImplementationConfigurationName()),
						project.getConfigurations().getByName(main.getRuntimeOnlyConfigurationName()));
			requiredClasspath.getDependencies()
				.add(project.getDependencies()
					.project(Map.of("path", ":spring-boot-project:spring-boot-autoconfigure")));
			TaskProvider<CheckAutoConfigurationClasses> checkAutoConfigurationClasses = project.getTasks()
				.register("checkAutoConfigurationClasses", CheckAutoConfigurationClasses.class, (task) -> {
					task.setSource(main.getResources());
					task.setClasspath(main.getOutput().getClassesDirs());
					task.setRequiredDependencies(requiredClasspath);
					task.setDescription("Checks the auto-configuration classes of the main source set.");
				});
			project.getPlugins()
				.withType(OptionalDependenciesPlugin.class,
						(plugin) -> checkAutoConfigurationClasses.configure((check) -> {
							Configuration optionalClasspath = project.getConfigurations()
								.create("autoConfigurationOptionalClassPath")
								.extendsFrom(project.getConfigurations()
									.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME));
							check.setOptionalDependencies(optionalClasspath);
						}));
		});
	}

}
