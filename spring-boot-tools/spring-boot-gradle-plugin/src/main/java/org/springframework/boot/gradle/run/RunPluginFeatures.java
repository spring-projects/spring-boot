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

package org.springframework.boot.gradle.run;

import java.util.Collections;
import java.util.concurrent.Callable;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import org.springframework.boot.gradle.MainClassResolver;
import org.springframework.boot.gradle.PluginFeatures;

/**
 * {@link PluginFeatures} to add run support.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class RunPluginFeatures implements PluginFeatures {

	private static final String RUN_APP_TASK_NAME = "bootRun";

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> {
			addBootRunTask(project);
		});
	}

	private void addBootRunTask(final Project project) {
		final JavaPluginConvention javaConvention = project.getConvention()
				.getPlugin(JavaPluginConvention.class);

		BootRun run = project.getTasks().create(RUN_APP_TASK_NAME, BootRun.class);
		run.setDescription("Run the project with support for "
				+ "auto-detecting main class and reloading static resources");
		run.setGroup("application");
		run.classpath(javaConvention.getSourceSets()
				.findByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath());
		run.getConventionMapping().map("jvmArgs", new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				if (project.hasProperty("applicationDefaultJvmArgs")) {
					return project.property("applicationDefaultJvmArgs");
				}
				return Collections.emptyList();
			}
		});
		run.conventionMapping("main", () -> {
			return new MainClassResolver(run.getClasspath()).resolveMainClass();
		});
	}

}
