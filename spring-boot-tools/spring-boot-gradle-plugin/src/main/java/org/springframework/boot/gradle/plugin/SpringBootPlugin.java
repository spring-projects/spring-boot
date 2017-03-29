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

package org.springframework.boot.gradle.plugin;

import java.util.Arrays;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.component.SoftwareComponent;

import org.springframework.boot.gradle.dsl.SpringBootExtension;
import org.springframework.boot.gradle.tasks.bundling.BootJar;
import org.springframework.boot.gradle.tasks.bundling.BootWar;

/**
 * Gradle plugin for Spring Boot.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class SpringBootPlugin implements Plugin<Project> {

	/**
	 * The name of the {@link Configuration} that contains Spring Boot archives.
	 */
	public static final String BOOT_ARCHIVES_CONFIURATION_NAME = "bootArchives";

	/**
	 * The name of the {@link SoftwareComponent} for a Spring Boot Java application.
	 */
	public static final String BOOT_JAVA_SOFTWARE_COMPONENT_NAME = "bootJava";

	/**
	 * The name of the {@link SoftwareComponent} for a Spring Boot Web application.
	 */
	public static final String BOOT_WEB_SOFTWARE_COMPONENT_NAME = "bootWeb";

	/**
	 * The name of the default {@link BootJar} task.
	 */
	public static final String BOOT_JAR_TASK_NAME = "bootJar";

	/**
	 * The name of the default {@link BootWar} task.
	 */
	public static final String BOOT_WAR_TASK_NAME = "bootWar";

	@Override
	public void apply(Project project) {
		project.getExtensions().create("springBoot", SpringBootExtension.class, project);
		Configuration bootArchives = project.getConfigurations()
				.create(BOOT_ARCHIVES_CONFIURATION_NAME);
		SinglePublishedArtifact singlePublishedArtifact = new SinglePublishedArtifact(
				bootArchives.getArtifacts());
		List<PluginApplicationAction> actions = Arrays.asList(
				new JavaPluginAction(singlePublishedArtifact),
				new WarPluginAction(singlePublishedArtifact),
				new MavenPluginAction(bootArchives.getUploadTaskName()),
				new DependencyManagementPluginAction(), new ApplicationPluginAction());
		for (PluginApplicationAction action : actions) {
			project.getPlugins().withType(action.getPluginClass(),
					plugin -> action.execute(project));
		}
	}

}
