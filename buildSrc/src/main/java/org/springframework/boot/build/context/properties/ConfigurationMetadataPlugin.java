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

package org.springframework.boot.build.context.properties;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.language.jvm.tasks.ProcessResources;

/**
 * {@link Plugin} for projects that <em>only</em> define manual configuration metadata.
 * When applied, the plugin registers a {@link CheckManualSpringConfigurationMetadata}
 * task and configures the {@code check} task to depend upon it.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class ConfigurationMetadataPlugin implements Plugin<Project> {

	private static final String CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME = "configurationPropertiesMetadata";

	/**
	 * Name of the {@link CheckAdditionalSpringConfigurationMetadata} task.
	 */
	public static final String CHECK_MANUAL_SPRING_CONFIGURATION_METADATA_TASK_NAME = "checkManualSpringConfigurationMetadata";

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> registerCheckAdditionalMetadataTask(project));
	}

	private void registerCheckAdditionalMetadataTask(Project project) {
		TaskProvider<CheckManualSpringConfigurationMetadata> checkConfigurationMetadata = project.getTasks()
			.register(CHECK_MANUAL_SPRING_CONFIGURATION_METADATA_TASK_NAME,
					CheckManualSpringConfigurationMetadata.class);
		SourceSet mainSourceSet = project.getExtensions()
			.getByType(JavaPluginExtension.class)
			.getSourceSets()
			.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		Provider<File> manualMetadataLocation = project.getTasks()
			.named(mainSourceSet.getProcessResourcesTaskName(), ProcessResources.class)
			.map((processResources) -> new File(processResources.getDestinationDir(),
					"META-INF/spring-configuration-metadata.json"));
		checkConfigurationMetadata.configure((check) -> {
			check.getMetadataLocation().set(manualMetadataLocation);
			check.getReportLocation()
				.set(project.getLayout()
					.getBuildDirectory()
					.file("reports/manual-spring-configuration-metadata/check.txt"));
		});
		addMetadataArtifact(project, manualMetadataLocation);
		project.getTasks()
			.named(LifecycleBasePlugin.CHECK_TASK_NAME)
			.configure((check) -> check.dependsOn(checkConfigurationMetadata));
	}

	private void addMetadataArtifact(Project project, Provider<File> metadataLocation) {
		project.getConfigurations()
			.consumable(CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME, (configuration) -> {
				configuration.attributes((attributes) -> {
					attributes.attribute(Category.CATEGORY_ATTRIBUTE,
							project.getObjects().named(Category.class, Category.DOCUMENTATION));
					attributes.attribute(Usage.USAGE_ATTRIBUTE,
							project.getObjects().named(Usage.class, "configuration-properties-metadata"));
				});
			});
		project.getArtifacts().add(CONFIGURATION_PROPERTIES_METADATA_CONFIGURATION_NAME, metadataLocation);
	}

}
