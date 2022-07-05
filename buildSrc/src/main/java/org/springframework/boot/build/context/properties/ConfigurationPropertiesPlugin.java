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

package org.springframework.boot.build.context.properties;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.file.Directory;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;

import org.springframework.util.StringUtils;

/**
 * {@link Plugin} for projects that define {@code @ConfigurationProperties}. When applied,
 * the plugin reacts to the presence of the {@link JavaPlugin} by:
 *
 * <ul>
 * <li>Adding a dependency on the configuration properties annotation processor.
 * <li>Configuring the additional metadata locations annotation processor compiler
 * argument.
 * <li>Adding the outputs of the processResources task as inputs of the compileJava task
 * to ensure that the additional metadata is available when the annotation processor runs.
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

	private void addMetadataArtifact (Project project) {
		ExtensionContainer extensionContainer = project.getExtensions();
		JavaPluginExtension javaPluginExtension = extensionContainer.getByType(JavaPluginExtension.class);
		SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

		project.getConfigurations().maybeCreate(CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME);

		project.afterEvaluate((evaluatedProject) -> {
			ArtifactHandler artifactHandler = evaluatedProject.getArtifacts();
			SourceDirectorySet sourceDirectorySet = mainSourceSet.getJava();
			Provider<Directory> directoryProvider = sourceDirectorySet.getDestinationDirectory().dir("META-INF/spring-configuration-metadata.json");

			artifactHandler.add(CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME, directoryProvider, (artifact) -> {
				TaskContainer taskContainer = evaluatedProject.getTasks();
				String taskName = mainSourceSet.getClassesTaskName();
				artifact.builtBy(taskContainer.getByName(taskName));

				try {
					checkIfSorted(artifact.getFile());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		});
	}

	private Boolean checkIfSorted(File file) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> json = objectMapper.readValue(file, Map.class);
		return checkByKey(json, "properties") && checkByKey(json, "hints") && checkByKey(json, "groups");
	}

	private Boolean checkByKey(Map<String, Object> json, String key){
		String previousName = "";
		for (Map<String, Object> property : (List<Map<String, Object>>) json.get(key)) {
			String name = (String) property.get("name");
			if (previousName != "" && name.compareTo(previousName) > 0)
				return false;
			previousName = name;
		}
		return true;
	}

	private void configureAdditionalMetadataLocationsCompilerArgument(Project project) {
		JavaCompile compileJava = project.getTasks().withType(JavaCompile.class)
				.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
		((Task) compileJava).getInputs().files(project.getTasks().getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME))
				.withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("processed resources");
		SourceSet mainSourceSet = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
				.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		compileJava.getOptions().getCompilerArgs()
				.add("-Aorg.springframework.boot.configurationprocessor.additionalMetadataLocations=" + StringUtils
						.collectionToCommaDelimitedString(mainSourceSet.getResources().getSourceDirectories().getFiles()
								.stream().map(project.getRootProject()::relativePath).collect(Collectors.toSet())));
	}

}
