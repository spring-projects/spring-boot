/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.build;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

/**
 * A plugin to make a project's {@code deployment} publication available as a Maven
 * repository. The repository can be consumed by depending upon the project using the
 * {@code mavenRepository} configuration.
 *
 * @author Andy Wilkinson
 */
public class MavenRepositoryPlugin implements Plugin<Project> {

	/**
	 * Name of the {@code mavenRepository} configuration.
	 */
	public static final String MAVEN_REPOSITORY_CONFIGURATION_NAME = "mavenRepository";

	/**
	 * Name of the task that publishes to the project repository.
	 */
	public static final String PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME = "publishMavenPublicationToProjectRepository";

	/**
	 * Applies the MavenPublishPlugin to the given project and sets up the project
	 * repository for publishing.
	 * @param project the project to apply the MavenPublishPlugin to
	 */
	@Override
	public void apply(Project project) {
		project.getPlugins().apply(MavenPublishPlugin.class);
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		File repositoryLocation = new File(project.getBuildDir(), "maven-repository");
		publishing.getRepositories().maven((mavenRepository) -> {
			mavenRepository.setName("project");
			mavenRepository.setUrl(repositoryLocation.toURI());
		});
		project.getTasks()
			.matching((task) -> task.getName().equals(PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME))
			.all((task) -> setUpProjectRepository(project, task, repositoryLocation));
		project.getTasks()
			.matching((task) -> task.getName().equals("publishPluginMavenPublicationToProjectRepository"))
			.all((task) -> setUpProjectRepository(project, task, repositoryLocation));
	}

	/**
	 * Sets up the project repository by performing the following tasks: 1. Cleans the
	 * repository location before publishing the project. 2. Creates a configuration for
	 * the project repository. 3. Adds the repository location as an artifact to the
	 * project. 4. Adds Maven repository dependencies to the project based on the plugin
	 * type.
	 * @param project The project for which the repository is being set up.
	 * @param publishTask The task responsible for publishing the project.
	 * @param repositoryLocation The location of the repository.
	 */
	private void setUpProjectRepository(Project project, Task publishTask, File repositoryLocation) {
		publishTask.doFirst(new CleanAction(repositoryLocation));
		Configuration projectRepository = project.getConfigurations().create(MAVEN_REPOSITORY_CONFIGURATION_NAME);
		project.getArtifacts()
			.add(projectRepository.getName(), repositoryLocation, (artifact) -> artifact.builtBy(publishTask));
		DependencySet target = projectRepository.getDependencies();
		project.getPlugins()
			.withType(JavaPlugin.class)
			.all((javaPlugin) -> addMavenRepositoryDependencies(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
					target));
		project.getPlugins()
			.withType(JavaLibraryPlugin.class)
			.all((javaLibraryPlugin) -> addMavenRepositoryDependencies(project, JavaPlugin.API_CONFIGURATION_NAME,
					target));
		project.getPlugins()
			.withType(JavaPlatformPlugin.class)
			.all((javaPlugin) -> addMavenRepositoryDependencies(project, JavaPlatformPlugin.API_CONFIGURATION_NAME,
					target));
	}

	/**
	 * Adds Maven repository dependencies to the target dependency set.
	 * @param project The project to which the dependencies will be added.
	 * @param sourceConfigurationName The name of the source configuration from which the
	 * dependencies will be retrieved.
	 * @param target The target dependency set to which the dependencies will be added.
	 */
	private void addMavenRepositoryDependencies(Project project, String sourceConfigurationName, DependencySet target) {
		project.getConfigurations()
			.getByName(sourceConfigurationName)
			.getDependencies()
			.withType(ProjectDependency.class)
			.all((dependency) -> {
				Map<String, String> dependencyDescriptor = new HashMap<>();
				dependencyDescriptor.put("path", dependency.getDependencyProject().getPath());
				dependencyDescriptor.put("configuration", MAVEN_REPOSITORY_CONFIGURATION_NAME);
				target.add(project.getDependencies().project(dependencyDescriptor));
			});
	}

	/**
	 * CleanAction class.
	 */
	private static final class CleanAction implements Action<Task> {

		private final File location;

		/**
		 * Constructs a new CleanAction object with the specified file location.
		 * @param location the file location to be cleaned
		 */
		private CleanAction(File location) {
			this.location = location;
		}

		/**
		 * Executes the CleanAction by deleting the project at the specified location.
		 * @param task the task to be executed
		 */
		@Override
		public void execute(Task task) {
			task.getProject().delete(this.location);
		}

	}

}
