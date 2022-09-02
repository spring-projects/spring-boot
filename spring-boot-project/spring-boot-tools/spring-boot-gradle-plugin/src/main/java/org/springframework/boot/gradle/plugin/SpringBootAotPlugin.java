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

import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.gradle.tasks.aot.AbstractAot;
import org.springframework.boot.gradle.tasks.aot.ProcessAot;
import org.springframework.boot.gradle.tasks.aot.ProcessTestAot;

/**
 * Gradle plugin for Spring Boot AOT.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
public class SpringBootAotPlugin implements Plugin<Project> {

	/**
	 * Name of the main {@code aot} {@link SourceSet source set}.
	 */
	public static final String AOT_SOURCE_SET_NAME = "aot";

	/**
	 * Name of the {@code aotTest} {@link SourceSet source set}.
	 */
	public static final String AOT_TEST_SOURCE_SET_NAME = "aotTest";

	/**
	 * Name of the default {@link ProcessAot} task.
	 */
	public static final String PROCESS_AOT_TASK_NAME = "processAot";

	/**
	 * Name of the default {@link ProcessAot} task.
	 */
	public static final String PROCESS_TEST_AOT_TASK_NAME = "processTestAot";

	@Override
	public void apply(Project project) {
		PluginContainer plugins = project.getPlugins();
		plugins.withType(JavaPlugin.class).all((javaPlugin) -> {
			plugins.withType(SpringBootPlugin.class).all((bootPlugin) -> {
				SourceSet aotSourceSet = configureSourceSet(project, "aot", SourceSet.MAIN_SOURCE_SET_NAME);
				registerProcessAotTask(project, aotSourceSet);
				SourceSet aotTestSourceSet = configureSourceSet(project, "aotTest", SourceSet.TEST_SOURCE_SET_NAME);
				registerProcessTestAotTask(project, aotTestSourceSet);
			});
		});
	}

	private SourceSet configureSourceSet(Project project, String newSourceSetName, String existingSourceSetName) {
		JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
		SourceSet existingSourceSet = sourceSets.getByName(existingSourceSetName);
		return sourceSets.create(newSourceSetName, (sourceSet) -> {
			sourceSet.getJava().setSrcDirs(List.of("build/generated/" + newSourceSetName + "Sources"));
			sourceSet.getResources().setSrcDirs(List.of("build/generated/" + newSourceSetName + "Resources"));
			sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(existingSourceSet.getOutput()));
			existingSourceSet.setRuntimeClasspath(existingSourceSet.getRuntimeClasspath().plus(sourceSet.getOutput()));
			ConfigurationContainer configurations = project.getConfigurations();
			Configuration implementation = configurations.getByName(sourceSet.getImplementationConfigurationName());
			implementation
					.extendsFrom(configurations.getByName(existingSourceSet.getImplementationConfigurationName()));
			implementation.extendsFrom(configurations.getByName(existingSourceSet.getRuntimeOnlyConfigurationName()));
			configurations.getByName(sourceSet.getCompileClasspathConfigurationName()).attributes((attributes) -> {
				configureClassesAndResourcesLibraryElementsAttribute(project, attributes);
				configureJavaRuntimeUsageAttribute(project, attributes);
			});
		});
	}

	private void configureClassesAndResourcesLibraryElementsAttribute(Project project, AttributeContainer attributes) {
		LibraryElements classesAndResources = project.getObjects().named(LibraryElements.class,
				LibraryElements.CLASSES_AND_RESOURCES);
		attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, classesAndResources);
	}

	private void configureJavaRuntimeUsageAttribute(Project project, AttributeContainer attributes) {
		Usage javaRuntime = project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME);
		attributes.attribute(Usage.USAGE_ATTRIBUTE, javaRuntime);
	}

	private void registerProcessAotTask(Project project, SourceSet aotSourceSet) {
		TaskProvider<ResolveMainClassName> resolveMainClassName = project.getTasks()
				.named(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class);
		TaskProvider<ProcessAot> processAot = project.getTasks().register(PROCESS_AOT_TASK_NAME, ProcessAot.class,
				(task) -> {
					configureAotTask(project, aotSourceSet, task,
							project.getLayout().getBuildDirectory().dir("generated/aotClasses"));
					task.getApplicationClass()
							.set(resolveMainClassName.flatMap(ResolveMainClassName::readMainClassName));
				});
		configureDependsOn(project, aotSourceSet, processAot);
	}

	private void configureAotTask(Project project, SourceSet sourceSet, AbstractAot task,
			Provider<Directory> generatedClasses) {
		task.setClasspath(sourceSet.getCompileClasspath());
		task.getSourcesDir().set(sourceSet.getJava().getSrcDirs().iterator().next());
		task.getResourcesDir().set(sourceSet.getResources().getSrcDirs().iterator().next());
		task.getClassesDir().set(generatedClasses);
		task.getGroupId().set(project.provider(() -> String.valueOf(project.getGroup())));
		task.getArtifactId().set(project.provider(() -> project.getName()));
		sourceSet.getOutput().dir(generatedClasses);
	}

	private void configureDependsOn(Project project, SourceSet aotSourceSet,
			TaskProvider<? extends AbstractAot> processAot) {
		project.getTasks().named(aotSourceSet.getCompileJavaTaskName())
				.configure((compileJava) -> compileJava.dependsOn(processAot));
		project.getTasks().named(aotSourceSet.getProcessResourcesTaskName())
				.configure((processResources) -> processResources.dependsOn(processAot));
	}

	private void registerProcessTestAotTask(Project project, SourceSet aotTestSourceSet) {
		JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
		SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
		TaskProvider<ProcessTestAot> processTestAot = project.getTasks().register(PROCESS_TEST_AOT_TASK_NAME,
				ProcessTestAot.class, (task) -> {
					configureAotTask(project, aotTestSourceSet, task,
							project.getLayout().getBuildDirectory().dir("generated/aotTestClasses"));
					task.setTestClassesDirs(testSourceSet.getOutput().getClassesDirs());
				});
		configureDependsOn(project, aotTestSourceSet, processTestAot);
	}

}
