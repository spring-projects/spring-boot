/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.bundling.Jar;

/**
 * A plugin applied to a project that should be deployed.
 *
 * @author Andy Wilkinson
 */
public class DeployedPlugin implements Plugin<Project> {

	/**
	 * Name of the task that generates the deployed pom file.
	 */
	public static final String GENERATE_POM_TASK_NAME = "generatePomFileForMavenPublication";

	@Override
	@SuppressWarnings("deprecation")
	public void apply(Project project) {
		project.getPlugins().apply(MavenPublishPlugin.class);
		project.getPlugins().apply(MavenRepositoryPlugin.class);
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		MavenPublication mavenPublication = publishing.getPublications().create("maven", MavenPublication.class);
		project.afterEvaluate((evaluated) -> project.getPlugins().withType(JavaPlugin.class).all((javaPlugin) -> {
			if (((Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME)).isEnabled()) {
				project.getComponents()
					.matching((component) -> component.getName().equals("java"))
					.all(mavenPublication::from);
			}
		}));
		project.getPlugins()
			.withType(JavaPlatformPlugin.class)
			.all((javaPlugin) -> project.getComponents()
				.matching((component) -> component.getName().equals("javaPlatform"))
				.all(mavenPublication::from));
	}

}
