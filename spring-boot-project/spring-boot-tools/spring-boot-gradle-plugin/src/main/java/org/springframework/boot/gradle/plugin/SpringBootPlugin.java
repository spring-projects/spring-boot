/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.util.GradleVersion;

import org.springframework.boot.gradle.dsl.SpringBootExtension;
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage;
import org.springframework.boot.gradle.tasks.bundling.BootJar;
import org.springframework.boot.gradle.tasks.bundling.BootWar;
import org.springframework.boot.gradle.util.VersionExtractor;

/**
 * Gradle plugin for Spring Boot.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Danny Hyun
 * @author Scott Frederick
 * @since 1.2.7
 */
public class SpringBootPlugin implements Plugin<Project> {

	private static final String SPRING_BOOT_VERSION = VersionExtractor.forClass(DependencyManagementPluginAction.class);

	/**
	 * The name of the {@link Configuration} that contains Spring Boot archives.
	 * @since 2.0.0
	 */
	public static final String BOOT_ARCHIVES_CONFIGURATION_NAME = "bootArchives";

	/**
	 * The name of the default {@link BootJar} task.
	 * @since 2.0.0
	 */
	public static final String BOOT_JAR_TASK_NAME = "bootJar";

	/**
	 * The name of the default {@link BootWar} task.
	 * @since 2.0.0
	 */
	public static final String BOOT_WAR_TASK_NAME = "bootWar";

	/**
	 * The name of the default {@link BootBuildImage} task.
	 * @since 2.3.0
	 */
	public static final String BOOT_BUILD_IMAGE_TASK_NAME = "bootBuildImage";

	/**
	 * The name of the {@code developmentOnly} configuration.
	 * @since 2.3.0
	 */
	public static final String DEVELOPMENT_ONLY_CONFIGURATION_NAME = "developmentOnly";

	/**
	 * The name of the {@code productionRuntimeClasspath} configuration.
	 */
	public static final String PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME = "productionRuntimeClasspath";

	/**
	 * The coordinates {@code (group:name:version)} of the
	 * {@code spring-boot-dependencies} bom.
	 */
	public static final String BOM_COORDINATES = "org.springframework.boot:spring-boot-dependencies:"
			+ SPRING_BOOT_VERSION;

	@Override
	public void apply(Project project) {
		verifyGradleVersion();
		createExtension(project);
		Configuration bootArchives = createBootArchivesConfiguration(project);
		registerPluginActions(project, bootArchives);
	}

	private void verifyGradleVersion() {
		GradleVersion currentVersion = GradleVersion.current();
		if (currentVersion.compareTo(GradleVersion.version("6.8")) < 0) {
			throw new GradleException("Spring Boot plugin requires Gradle 6.8.x, 6.9.x, or 7.x. "
					+ "The current version is " + currentVersion);
		}
	}

	private void createExtension(Project project) {
		project.getExtensions().create("springBoot", SpringBootExtension.class, project);
	}

	private Configuration createBootArchivesConfiguration(Project project) {
		Configuration bootArchives = project.getConfigurations().create(BOOT_ARCHIVES_CONFIGURATION_NAME);
		bootArchives.setDescription("Configuration for Spring Boot archive artifacts.");
		bootArchives.setCanBeResolved(false);
		return bootArchives;
	}

	private void registerPluginActions(Project project, Configuration bootArchives) {
		SinglePublishedArtifact singlePublishedArtifact = new SinglePublishedArtifact(bootArchives,
				project.getArtifacts());
		@SuppressWarnings("deprecation")
		List<PluginApplicationAction> actions = Arrays.asList(new JavaPluginAction(singlePublishedArtifact),
				new WarPluginAction(singlePublishedArtifact), new MavenPluginAction(bootArchives.getUploadTaskName()),
				new DependencyManagementPluginAction(), new ApplicationPluginAction(), new KotlinPluginAction());
		for (PluginApplicationAction action : actions) {
			withPluginClassOfAction(action,
					(pluginClass) -> project.getPlugins().withType(pluginClass, (plugin) -> action.execute(project)));
		}
	}

	private void withPluginClassOfAction(PluginApplicationAction action,
			Consumer<Class<? extends Plugin<? extends Project>>> consumer) {
		try {
			consumer.accept(action.getPluginClass());
		}
		catch (Throwable ex) {
			// Plugin class unavailable. Continue.
		}
	}

}
