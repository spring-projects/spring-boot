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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.build.DeployedPlugin;
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
 * <li>Add checks to ensure import files and annotations are correct</li>
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
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> new Configurer(project).configure());
	}

	private static class Configurer {

		private final Project project;

		private SourceSet main;

		Configurer(Project project) {
			this.project = project;
			this.main = project.getExtensions()
				.getByType(JavaPluginExtension.class)
				.getSourceSets()
				.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		}

		void configure() {
			addAnnotationProcessorsDependencies();
			TaskContainer tasks = this.project.getTasks();
			ConfigurationContainer configurations = this.project.getConfigurations();
			tasks.register("autoConfigurationMetadata", AutoConfigurationMetadata.class,
					this::configureAutoConfigurationMetadata);
			TaskProvider<CheckAutoConfigurationImports> checkAutoConfigurationImports = tasks.register(
					"checkAutoConfigurationImports", CheckAutoConfigurationImports.class,
					this::configureCheckAutoConfigurationImports);
			Configuration requiredClasspath = configurations.create("autoConfigurationRequiredClasspath")
				.extendsFrom(configurations.getByName(this.main.getImplementationConfigurationName()),
						configurations.getByName(this.main.getRuntimeOnlyConfigurationName()));
			requiredClasspath.getDependencies()
				.add(projectDependency(":spring-boot-project:spring-boot-autoconfigure"));
			TaskProvider<CheckAutoConfigurationClasses> checkAutoConfigurationClasses = tasks.register(
					"checkAutoConfigurationClasses", CheckAutoConfigurationClasses.class,
					(task) -> configureCheckAutoConfigurationClasses(requiredClasspath, task));
			this.project.getPlugins()
				.withType(OptionalDependenciesPlugin.class,
						(plugin) -> configureCheckAutoConfigurationClassesForOptionalDependencies(configurations,
								checkAutoConfigurationClasses));
			this.project.getTasks()
				.getByName(JavaBasePlugin.CHECK_TASK_NAME)
				.dependsOn(checkAutoConfigurationImports, checkAutoConfigurationClasses);
		}

		private void addAnnotationProcessorsDependencies() {
			this.project.getConfigurations()
				.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
				.getDependencies()
				.addAll(projectDependencies(
						":spring-boot-project:spring-boot-tools:spring-boot-autoconfigure-processor",
						":spring-boot-project:spring-boot-tools:spring-boot-configuration-processor"));
		}

		private void configureAutoConfigurationMetadata(AutoConfigurationMetadata task) {
			task.setSourceSet(this.main);
			task.dependsOn(this.main.getClassesTaskName());
			task.getOutputFile()
				.set(this.project.getLayout().getBuildDirectory().file("auto-configuration-metadata.properties"));
			this.project.getArtifacts()
				.add(AutoConfigurationPlugin.AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME, task.getOutputFile(),
						(artifact) -> artifact.builtBy(task));
		}

		private void configureCheckAutoConfigurationImports(CheckAutoConfigurationImports task) {
			task.setSource(this.main.getResources());
			task.setClasspath(this.main.getOutput().getClassesDirs());
			task.setDescription(
					"Checks the %s file of the main source set.".formatted(AutoConfigurationImportsTask.IMPORTS_FILE));
		}

		private void configureCheckAutoConfigurationClasses(Configuration requiredClasspath,
				CheckAutoConfigurationClasses task) {
			task.setSource(this.main.getResources());
			task.setClasspath(this.main.getOutput().getClassesDirs());
			task.setRequiredDependencies(requiredClasspath);
			task.setDescription("Checks the auto-configuration classes of the main source set.");
		}

		private void configureCheckAutoConfigurationClassesForOptionalDependencies(
				ConfigurationContainer configurations,
				TaskProvider<CheckAutoConfigurationClasses> checkAutoConfigurationClasses) {
			checkAutoConfigurationClasses.configure((check) -> {
				Configuration optionalClasspath = configurations.create("autoConfigurationOptionalClassPath")
					.extendsFrom(configurations.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME));
				check.setOptionalDependencies(optionalClasspath);
			});
		}

		private Set<Dependency> projectDependencies(String... paths) {
			return Arrays.stream(paths).map((path) -> projectDependency(path)).collect(Collectors.toSet());
		}

		private Dependency projectDependency(String path) {
			return this.project.getDependencies().project(Collections.singletonMap("path", path));
		}

	}

}
