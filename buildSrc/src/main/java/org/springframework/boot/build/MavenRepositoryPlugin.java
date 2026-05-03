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

package org.springframework.boot.build;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

import org.springframework.util.FileSystemUtils;

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

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(MavenPublishPlugin.class);
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		File repositoryLocation = project.getLayout().getBuildDirectory().dir("maven-repository").get().getAsFile();
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

	private void setUpProjectRepository(Project project, Task publishTask, File repositoryLocation) {
		publishTask.doFirst(new CleanAction(repositoryLocation));
		Configuration projectRepository = project.getConfigurations().create(MAVEN_REPOSITORY_CONFIGURATION_NAME);
		project.getArtifacts()
			.add(projectRepository.getName(), repositoryLocation, (artifact) -> artifact.builtBy(publishTask));
		DependencySet target = projectRepository.getDependencies();
		project.getPlugins()
			.withType(JavaPlugin.class)
			.all((javaPlugin) -> addMavenRepositoryProjectDependencies(project,
					JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, target));
		project.getPlugins()
			.withType(JavaLibraryPlugin.class)
			.all((javaLibraryPlugin) -> addMavenRepositoryProjectDependencies(project,
					JavaPlugin.API_CONFIGURATION_NAME, target));
		project.getPlugins().withType(JavaPlatformPlugin.class).all((javaPlugin) -> {
			addMavenRepositoryProjectDependencies(project, JavaPlatformPlugin.API_CONFIGURATION_NAME, target);
			addMavenRepositoryPlatformDependencies(project, JavaPlatformPlugin.API_CONFIGURATION_NAME, target);
		});
	}

	private void addMavenRepositoryProjectDependencies(Project project, String sourceConfigurationName,
			DependencySet target) {
		project.getConfigurations()
			.getByName(sourceConfigurationName)
			.getDependencies()
			.withType(ProjectDependency.class)
			.all((dependency) -> {
				ProjectDependency copy = dependency.copy();
				if (copy.getAttributes().isEmpty()) {
					copy.setTargetConfiguration(MAVEN_REPOSITORY_CONFIGURATION_NAME);
				}
				target.add(copy);
			});
	}

	private void addMavenRepositoryPlatformDependencies(Project project, String sourceConfigurationName,
			DependencySet target) {
		project.getConfigurations()
			.getByName(sourceConfigurationName)
			.getDependencies()
			.withType(ModuleDependency.class)
			.matching((dependency) -> {
				Category category = dependency.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
				return Category.REGULAR_PLATFORM.equals(category.getName());
			})
			.all((dependency) -> {
				Dependency pom = project.getDependencies()
					.create(dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion());
				target.add(pom);
			});
	}

	private static final class CleanAction implements Action<Task> {

		private final File location;

		private CleanAction(File location) {
			this.location = location;
		}

		@Override
		public void execute(Task task) {
			FileSystemUtils.deleteRecursively(this.location);
		}

	}

}
