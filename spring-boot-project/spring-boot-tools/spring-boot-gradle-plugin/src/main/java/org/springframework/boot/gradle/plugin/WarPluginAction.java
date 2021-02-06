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

package org.springframework.boot.gradle.plugin;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.gradle.tasks.bundling.BootBuildImage;
import org.springframework.boot.gradle.tasks.bundling.BootWar;
import org.springframework.boot.gradle.tasks.bundling.BundledLibraries;
import org.springframework.boot.loader.tools.BundledLibrariesWriter;

/**
 * {@link Action} that is executed in response to the {@link WarPlugin} being applied.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class WarPluginAction implements PluginApplicationAction {

	private static final String BUNDLED_LIBRARIES_TASK_NAME = "bootWarBundledLibraries";

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
		disableWarTask(project);
		TaskProvider<BootWar> bootWar = configureBootWarTask(project);
		configureBootBuildImageTask(project, bootWar);
		configureArtifactPublication(bootWar);
	}

	private void disableWarTask(Project project) {
		project.getTasks().named(WarPlugin.WAR_TASK_NAME).configure((war) -> {
			war.setEnabled(false);
			war.mustRunAfter(BUNDLED_LIBRARIES_TASK_NAME);
		});
	}

	private TaskProvider<BootWar> configureBootWarTask(Project project) {
		Configuration developmentOnly = project.getConfigurations()
				.getByName(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		Configuration productionRuntimeClasspath = project.getConfigurations()
				.getByName(SpringBootPlugin.PRODUCTION_RUNTIME_CLASSPATH_NAME);
		FileCollection providedRuntimeClasspath = providedRuntimeConfiguration(project);
		FileCollection classpath = project.getConvention().getByType(SourceSetContainer.class)
				.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath().minus(providedRuntimeClasspath)
				.minus(developmentOnly.minus(productionRuntimeClasspath)).filter(new JarTypeFileSpec());

		TaskProvider<ResolveMainClassName> resolveMainClassName = ResolveMainClassName
				.registerForTask(SpringBootPlugin.BOOT_WAR_TASK_NAME, project, classpath);

		TaskProvider<BundledLibraries> bundledLibrariesTaskProvider = project.getTasks()
				.register(BUNDLED_LIBRARIES_TASK_NAME, BundledLibraries.class, (bundledLibraries) -> {
					bundledLibraries.setDescription(
							"Generates a META-INF/" + BundledLibrariesWriter.BUNDLED_LIBRARIES_FILE_NAME + " file.");
					bundledLibraries.setGroup(BasePlugin.BUILD_GROUP);
					bundledLibraries.setClasspath(classpath.plus(providedRuntimeClasspath));
					bundledLibraries.getOutputFile()
							.set(new File(
									project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
											.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getResourcesDir(),
									"META-INF/" + BundledLibrariesWriter.BUNDLED_LIBRARIES_FILE_NAME));
					bundledLibraries.mustRunAfter(resolveMainClassName);
				});

		TaskProvider<BootWar> bootWarProvider = project.getTasks().register(SpringBootPlugin.BOOT_WAR_TASK_NAME,
				BootWar.class, (bootWar) -> {
					bootWar.setGroup(BasePlugin.BUILD_GROUP);
					bootWar.setDescription("Assembles an executable war archive containing webapp"
							+ " content, and the main classes and their dependencies.");
					bootWar.providedClasspath(providedRuntimeClasspath);
					bootWar.setClasspath(classpath);
					Provider<String> manifestStartClass = project
							.provider(() -> (String) bootWar.getManifest().getAttributes().get("Start-Class"));
					bootWar.getMainClass()
							.convention(resolveMainClassName.flatMap((resolver) -> manifestStartClass.isPresent()
									? manifestStartClass : resolveMainClassName.get().readMainClassName()));
					bootWar.dependsOn(bundledLibrariesTaskProvider);
				});
		bootWarProvider.map((bootWar) -> bootWar.getClasspath());
		return bootWarProvider;
	}

	private FileCollection providedRuntimeConfiguration(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		return configurations.getByName(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME);
	}

	private void configureBootBuildImageTask(Project project, TaskProvider<BootWar> bootWar) {
		project.getTasks().named(SpringBootPlugin.BOOT_BUILD_IMAGE_TASK_NAME, BootBuildImage.class)
				.configure((buildImage) -> buildImage.getArchiveFile().set(bootWar.get().getArchiveFile()));
	}

	private void configureArtifactPublication(TaskProvider<BootWar> bootWar) {
		LazyPublishArtifact artifact = new LazyPublishArtifact(bootWar);
		this.singlePublishedArtifact.addCandidate(artifact);
	}

}
