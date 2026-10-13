/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build;

import org.antora.gradle.AntoraPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

/**
 * Plugin to apply conventions to projects that are part of Spring Boot's build.
 * Conventions are applied in response to various plugins being applied.
 *
 * When the {@link JavaBasePlugin} is applied, the conventions in {@link JavaConventions}
 * are applied.
 *
 * When the {@link MavenPublishPlugin} is applied, the conventions in
 * {@link MavenPublishingConventions} are applied.
 *
 * When the {@link AntoraPlugin} is applied, the conventions in {@link AntoraConventions}
 * are applied.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 */
public class ConventionsPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		SystemRequirementsExtension systemRequirements = project.getExtensions()
			.create("systemRequirements", SystemRequirementsExtension.class);
		new NoHttpConventions().apply(project);
		new JavaConventions(systemRequirements).apply(project);
		new MavenPublishingConventions().apply(project);
		new AntoraConventions().apply(project);
		new KotlinConventions().apply(project);
		new WarConventions().apply(project);
		new EclipseConventions(systemRequirements).apply(project);
		new TestFixturesConventions().apply(project);
		new ProtobufConventions().apply(project);
		RepositoryTransformersExtension.apply(project);
	}

}
