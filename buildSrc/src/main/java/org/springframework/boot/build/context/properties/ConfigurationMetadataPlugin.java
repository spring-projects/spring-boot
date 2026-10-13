/*
 * Copyright 2012-present the original author or authors.
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
