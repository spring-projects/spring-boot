/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.bundling;

import java.util.concurrent.Callable;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.SourceSet;

import org.springframework.boot.gradle.PluginFeatures;

/**
 * {@link PluginFeatures} for the bundling of an application.
 *
 * @author Andy Wilkinson
 */
public class BundlingPluginFeatures implements PluginFeatures {

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class,
				(javaPlugin) -> configureBootJarTask(project));
		project.getPlugins().withType(WarPlugin.class,
				(warPlugin) -> configureBootWarTask(project));
	}

	private void configureBootWarTask(Project project) {
		BootWar bootWar = project.getTasks().create("bootWar", BootWar.class);
		bootWar.providedClasspath(providedRuntimeConfiguration(project));
	}

	private void configureBootJarTask(Project project) {
		BootJar bootJar = project.getTasks().create("bootJar", BootJar.class);
		bootJar.classpath((Callable<FileCollection>) () -> {
			JavaPluginConvention convention = project.getConvention()
					.getPlugin(JavaPluginConvention.class);
			SourceSet mainSourceSet = convention.getSourceSets()
					.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			return mainSourceSet.getRuntimeClasspath();
		});
	}

	private Configuration providedRuntimeConfiguration(Project project) {
		return project.getConfigurations()
				.getByName(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME);
	}

}
