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

package org.springframework.boot.gradle.plugin;

import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.War;

import org.springframework.boot.gradle.tasks.bundling.BootBuildImage;
import org.springframework.boot.gradle.tasks.bundling.BootWar;

/**
 * {@link Action} that is executed in response to the {@link WarPlugin} being applied.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class WarPluginAction implements PluginApplicationAction {

	private final SinglePublishedArtifact singlePublishedArtifact;

	WarPluginAction(SinglePublishedArtifact singlePublishedArtifact) {
		this.singlePublishedArtifact = singlePublishedArtifact;
	}

	@Override
	public Class<? extends Plugin<? extends Project>> getPluginClass() {
		return WarPlugin.class;
	}

	@Override
	public void execute(Project project) {
		classifyWarTask(project);
		TaskProvider<BootWar> bootWar = configureBootWarTask(project);
		configureBootBuildImageTask(project, bootWar);
		configureArtifactPublication(bootWar);
	}

	private void classifyWarTask(Project project) {
		project.getTasks()
			.named(WarPlugin.WAR_TASK_NAME, War.class)
			.configure((war) -> war.getArchiveClassifier().convention("plain"));
	}

	private TaskProvider<BootWar> configureBootWarTask(Project project) {
		Configuration developmentOnly = project.getConfigurations()
			.getByName(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		Configuration testAndDevelopmentOnly = project.getConfigurations()
			.getByName(SpringBootPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		Configuration productionRuntimeClasspath = project.getConfigurations()
			.getByName(SpringBootPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
		SourceSet mainSourceSet = project.getExtensions()
			.getByType(SourceSetContainer.class)
			.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		Configuration runtimeClasspath = project.getConfigurations()
			.getByName(mainSourceSet.getRuntimeClasspathConfigurationName());
		Callable<FileCollection> classpath = () -> mainSourceSet.getRuntimeClasspath()
			.minus(providedRuntimeConfiguration(project))
			.minus((developmentOnly.minus(productionRuntimeClasspath)))
			.minus((testAndDevelopmentOnly.minus(productionRuntimeClasspath)))
			.filter(new JarTypeFileSpec());
		TaskProvider<ResolveMainClassName> resolveMainClassName = project.getTasks()
			.named(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class);
		TaskProvider<BootWar> bootWarProvider = project.getTasks()
			.register(SpringBootPlugin.BOOT_WAR_TASK_NAME, BootWar.class, (bootWar) -> {
				bootWar.setGroup(BasePlugin.BUILD_GROUP);
				bootWar.setDescription("Assembles an executable war archive containing webapp"
						+ " content, and the main classes and their dependencies.");
				bootWar.providedClasspath(providedRuntimeConfiguration(project));
				bootWar.setClasspath(classpath);
				Provider<String> manifestStartClass = project
					.provider(() -> (String) bootWar.getManifest().getAttributes().get("Start-Class"));
				bootWar.getMainClass()
					.convention(resolveMainClassName.flatMap((resolver) -> manifestStartClass.isPresent()
							? manifestStartClass : resolver.readMainClassName()));
				bootWar.getTargetJavaVersion()
					.set(project.provider(() -> javaPluginExtension(project).getTargetCompatibility()));
				bootWar.resolvedArtifacts(runtimeClasspath.getIncoming().getArtifacts().getResolvedArtifacts());
			});
		return bootWarProvider;
	}

	private FileCollection providedRuntimeConfiguration(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		return configurations.getByName(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME);
	}

	private void configureBootBuildImageTask(Project project, TaskProvider<BootWar> bootWar) {
		project.getTasks()
			.named(SpringBootPlugin.BOOT_BUILD_IMAGE_TASK_NAME, BootBuildImage.class)
			.configure((buildImage) -> buildImage.getArchiveFile().set(bootWar.get().getArchiveFile()));
	}

	private void configureArtifactPublication(TaskProvider<BootWar> bootWar) {
		this.singlePublishedArtifact.addWarCandidate(bootWar);
	}

	private JavaPluginExtension javaPluginExtension(Project project) {
		return project.getExtensions().getByType(JavaPluginExtension.class);
	}

}
