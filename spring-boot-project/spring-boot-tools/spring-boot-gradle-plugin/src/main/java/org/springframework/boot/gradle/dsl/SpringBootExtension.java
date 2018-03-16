/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.gradle.dsl;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;

import org.springframework.boot.gradle.tasks.buildinfo.BuildInfo;
import org.springframework.boot.gradle.tasks.buildinfo.BuildInfoProperties;

/**
 * Entry point to Spring Boot's Gradle DSL.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class SpringBootExtension {

	private final Project project;

	private String mainClassName;

	/**
	 * Creates a new {@code SpringBootPluginExtension} that is associated with the given
	 * {@code project}.
	 * @param project the project
	 */
	public SpringBootExtension(Project project) {
		this.project = project;
	}

	/**
	 * Returns the main class name of the application.
	 * @return the name of the application's main class
	 */
	public String getMainClassName() {
		return this.mainClassName;
	}

	/**
	 * Sets the main class name of the application.
	 * @param mainClassName the name of the application's main class
	 */
	public void setMainClassName(String mainClassName) {
		this.mainClassName = mainClassName;
	}

	/**
	 * Creates a new {@link BuildInfo} task named {@code bootBuildInfo} and configures the
	 * Java plugin's {@code classes} task to depend upon it.
	 * <p>
	 * By default, the task's destination dir will be a directory named {@code META-INF}
	 * beneath the main source set's resources output directory, and the task's project
	 * artifact will be the base name of the {@code bootWar} or {@code bootJar} task.
	 */
	public void buildInfo() {
		this.buildInfo(null);
	}

	/**
	 * Creates a new {@link BuildInfo} task named {@code bootBuildInfo} and configures the
	 * Java plugin's {@code classes} task to depend upon it. The task is passed to the
	 * given {@code configurer} for further configuration.
	 * <p>
	 * By default, the task's destination dir will be a directory named {@code META-INF}
	 * beneath the main source set's resources output directory, and the task's project
	 * artifact will be the base name of the {@code bootWar} or {@code bootJar} task.
	 * @param configurer the task configurer
	 */
	public void buildInfo(Action<BuildInfo> configurer) {
		BuildInfo bootBuildInfo = this.project.getTasks().create("bootBuildInfo",
				BuildInfo.class);
		bootBuildInfo.setGroup(BasePlugin.BUILD_GROUP);
		bootBuildInfo.setDescription("Generates a META-INF/build-info.properties file.");
		this.project.getPlugins().withType(JavaPlugin.class, (plugin) -> {
			this.project.getTasks().getByName(JavaPlugin.CLASSES_TASK_NAME)
					.dependsOn(bootBuildInfo);
			this.project.afterEvaluate((evaluated) -> {
				BuildInfoProperties properties = bootBuildInfo.getProperties();
				if (properties.getArtifact() == null) {
					properties.setArtifact(determineArtifactBaseName());
				}
			});
			bootBuildInfo.getConventionMapping().map("destinationDir",
					() -> new File(determineMainSourceSetResourcesOutputDir(),
							"META-INF"));
		});
		if (configurer != null) {
			configurer.execute(bootBuildInfo);
		}
	}

	private File determineMainSourceSetResourcesOutputDir() {
		return this.project.getConvention().getPlugin(JavaPluginConvention.class)
				.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput()
				.getResourcesDir();
	}

	private String determineArtifactBaseName() {
		Jar artifactTask = findArtifactTask();
		return (artifactTask == null ? null : artifactTask.getBaseName());
	}

	private Jar findArtifactTask() {
		Jar artifactTask = (Jar) this.project.getTasks().findByName("bootWar");
		if (artifactTask != null) {
			return artifactTask;
		}
		return (Jar) this.project.getTasks().findByName("bootJar");
	}

}
