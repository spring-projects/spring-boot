/*
 * Copyright 2020-2020 the original author or authors.
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

import java.util.Collections;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;

import org.springframework.boot.build.optional.OptionalDependenciesPlugin;

/**
 * Plugin to apply internal dependency management to Spring Boot's projects. Uses a custom
 * configuration to enforce a platform for Spring Boot's own build. This prevents the
 * enforced (strict) constraints from being visible to external consumers.
 *
 * @author Andy Wilkinson
 */
public class InternalDependencyManagementPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> configureDependencyManagement(project));
	}

	private void configureDependencyManagement(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		Configuration dependencyManagement = configurations.create("internalDependencyManagement", (configuration) -> {
			configuration.setVisible(false);
			configuration.setCanBeConsumed(false);
			configuration.setCanBeResolved(false);
		});
		configurations.matching((configuration) -> configuration.getName().endsWith("Classpath"))
				.all((configuration) -> configuration.extendsFrom(dependencyManagement));
		Dependency springBootParent = project.getDependencies().enforcedPlatform(project.getDependencies()
				.project(Collections.singletonMap("path", ":spring-boot-project:spring-boot-parent")));
		dependencyManagement.getDependencies().add(springBootParent);
		project.getPlugins().withType(OptionalDependenciesPlugin.class, (optionalDependencies) -> configurations
				.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME).extendsFrom(dependencyManagement));
	}

}
