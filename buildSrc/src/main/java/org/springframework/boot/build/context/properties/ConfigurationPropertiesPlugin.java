/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.build.context.properties;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import org.springframework.util.StringUtils;

/**
 * {@link Plugin} for projects that define {@code @ConfigurationProperties}. When applied,
 * the plugin reacts to the presence of the {@link JavaPlugin} by:
 *
 * <ul>
 * <li>Adding a dependency on the configuration properties annotation processor.
 * <li>Configure the additional metadata locations annotation processor compiler argument
 * <li>Defining an artifact for the resulting configuration property metadata so that it
 * can be consumed by downstream projects.
 * </ul>
 *
 * @author Andy Wilkinson
 */
public class ConfigurationPropertiesPlugin implements Plugin<Project> {

	/**
	 * Name of the {@link Configuration} that holds the configuration property metadata
	 * artifact.
	 */
	public static final String CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME = "configurationPropertiesMetadata";

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> {
			addConfigurationProcessorDependency(project);
			configureAdditionalMetadataLocationsCompilerArgument(project);
			addMetadataArtifact(project);
		});
	}

	private void addConfigurationProcessorDependency(Project project) {
		Configuration annotationProcessors = project.getConfigurations()
				.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
		annotationProcessors.getDependencies().add(project.getDependencies().project(Collections.singletonMap("path",
				":spring-boot-project:spring-boot-tools:spring-boot-configuration-processor")));
	}

	private void addMetadataArtifact(Project project) {
		SourceSet mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
				.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		project.getConfigurations().maybeCreate(CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME);
		project.afterEvaluate((evaluatedProject) -> evaluatedProject.getArtifacts().add(
				CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME,
				evaluatedProject.provider((Callable<File>) () -> new File(mainSourceSet.getJava().getOutputDir(),
						"META-INF/spring-configuration-metadata.json")),
				(artifact) -> artifact
						.builtBy(evaluatedProject.getTasks().getByName(mainSourceSet.getClassesTaskName()))));
	}

	private void configureAdditionalMetadataLocationsCompilerArgument(Project project) {
		JavaCompile compileJava = project.getTasks().withType(JavaCompile.class)
				.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
		SourceSet mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
				.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		compileJava.getOptions().getCompilerArgs()
				.add("-Aorg.springframework.boot.configurationprocessor.additionalMetadataLocations=" + StringUtils
						.collectionToCommaDelimitedString(mainSourceSet.getResources().getSourceDirectories().getFiles()
								.stream().map(project.getRootProject()::relativePath).collect(Collectors.toSet())));
	}

}
