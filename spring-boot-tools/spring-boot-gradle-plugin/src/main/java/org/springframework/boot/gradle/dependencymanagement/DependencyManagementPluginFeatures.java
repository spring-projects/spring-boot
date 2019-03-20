/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.gradle.dependencymanagement;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.dsl.ImportsHandler;
import org.gradle.api.Action;
import org.gradle.api.Project;

import org.springframework.boot.gradle.PluginFeatures;

/**
 * {@link PluginFeatures} to configure dependency management.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 1.3.0
 */
public class DependencyManagementPluginFeatures implements PluginFeatures {

	private static final String SPRING_BOOT_VERSION = DependencyManagementPluginFeatures.class
			.getPackage().getImplementationVersion();

	private static final String SPRING_BOOT_BOM = "org.springframework.boot:spring-boot-starter-parent:"
			+ SPRING_BOOT_VERSION;

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(DependencyManagementPlugin.class);
		DependencyManagementExtension dependencyManagement = project.getExtensions()
				.findByType(DependencyManagementExtension.class);
		dependencyManagement.imports(new Action<ImportsHandler>() {

			@Override
			public void execute(ImportsHandler importsHandler) {
				importsHandler.mavenBom(SPRING_BOOT_BOM);
			}

		});
	}

}
