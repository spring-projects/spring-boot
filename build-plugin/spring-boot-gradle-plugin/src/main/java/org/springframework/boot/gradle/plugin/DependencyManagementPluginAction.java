/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.plugin;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import org.springframework.util.Assert;

/**
 * {@link Action} that is performed in response to the {@link DependencyManagementPlugin}
 * being applied.
 *
 * @author Andy Wilkinson
 */
final class DependencyManagementPluginAction implements PluginApplicationAction {

	@Override
	public void execute(Project project) {
		DependencyManagementExtension extension = project.getExtensions()
			.findByType(DependencyManagementExtension.class);
		Assert.state(extension != null, "'extension' must not be null");
		extension.imports((importsHandler) -> importsHandler.mavenBom(SpringBootPlugin.BOM_COORDINATES));
	}

	@Override
	public Class<? extends Plugin<Project>> getPluginClass() {
		return DependencyManagementPlugin.class;
	}

}
