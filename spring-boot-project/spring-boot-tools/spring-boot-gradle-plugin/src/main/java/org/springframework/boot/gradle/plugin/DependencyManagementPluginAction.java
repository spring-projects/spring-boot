/*
 * Copyright 2012-2019 the original author or authors.
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

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * {@link Action} that is performed in response to the {@link DependencyManagementPlugin}
 * being applied.
 *
 * @author Andy Wilkinson
 */
final class DependencyManagementPluginAction implements PluginApplicationAction {

	@Override
	public void execute(Project project) {
		project.getExtensions().findByType(DependencyManagementExtension.class)
				.imports((importsHandler) -> importsHandler.mavenBom(SpringBootPlugin.BOM_COORDINATES));
	}

	@Override
	public Class<? extends Plugin<Project>> getPluginClass() {
		return DependencyManagementPlugin.class;
	}

}
