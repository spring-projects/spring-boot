/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.gradle.agent.AgentPluginFeatures;
import org.springframework.boot.gradle.dependencymanagement.DependencyManagementPluginFeatures;
import org.springframework.boot.gradle.repackage.RepackagePluginFeatures;
import org.springframework.boot.gradle.run.RunPluginFeatures;

/**
 * Gradle 'Spring Boot' {@link Plugin}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class SpringBootPlugin implements Plugin<Project> {

	private static final Logger logger = LoggerFactory.getLogger(SpringBootPlugin.class);

	@Override
	public void apply(Project project) {
		if (GradleVersion.current().compareTo(GradleVersion.version("2.8")) < 0) {
			logger.warn("Spring Boot plugin's support for Gradle "
					+ GradleVersion.current().getVersion()
					+ " is deprecated. Please upgrade to Gradle 2.8 or later.");
		}
		project.getExtensions().create("springBoot", SpringBootPluginExtension.class,
				project);
		project.getPlugins().apply(JavaPlugin.class);
		new AgentPluginFeatures().apply(project);
		new RepackagePluginFeatures().apply(project);
		new RunPluginFeatures().apply(project);
		new DependencyManagementPluginFeatures().apply(project);
		project.getTasks().withType(JavaCompile.class).all(new SetUtf8EncodingAction());
	}

	private static class SetUtf8EncodingAction implements Action<JavaCompile> {

		@Override
		public void execute(final JavaCompile compile) {
			compile.doFirst(new Action<Task>() {

				@Override
				@SuppressWarnings("deprecation")
				public void execute(Task t) {
					if (compile.getOptions().getEncoding() == null) {
						compile.getOptions().setEncoding("UTF-8");
					}
				}

			});
		}

	}

}
