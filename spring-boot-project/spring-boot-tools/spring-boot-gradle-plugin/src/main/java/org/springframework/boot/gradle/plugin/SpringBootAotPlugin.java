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
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.gradle.tasks.aot.GenerateAotSources;

/**
 * Gradle plugin for Spring Boot AOT.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
public class SpringBootAotPlugin implements Plugin<Project> {

	/**
	 * Name of the {@code aot} {@link SourceSet source set}.
	 */
	public static final String AOT_SOURCE_SET_NAME = "aot";

	/**
	 * Name of the default {@link GenerateAotSources} task.
	 */
	public static final String GENERATE_AOT_SOURCES_TASK_NAME = "generateAotSources";

	@Override
	public void apply(Project project) {
		PluginContainer plugins = project.getPlugins();
		plugins.withType(JavaPlugin.class).all((javaPlugin) -> {
			plugins.withType(SpringBootPlugin.class).all((bootPlugin) -> {
				SourceSet aotSourceSet = configureAotSourceSet(project);
				registerGenerateAotSourcesTask(project, aotSourceSet);
			});
		});
	}

	private SourceSet configureAotSourceSet(Project project) {
		JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
		SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		SourceSet aotSourceSet = sourceSets.create(AOT_SOURCE_SET_NAME, (aot) -> {
			aot.getJava().setSrcDirs(List.of("build/generated/aotSources"));
			aot.getResources().setSrcDirs(List.of("build/generated/aotResources"));
			aot.setCompileClasspath(aot.getCompileClasspath().plus(main.getOutput()));
			main.setRuntimeClasspath(main.getRuntimeClasspath().plus(aot.getOutput()));
			ConfigurationContainer configurations = project.getConfigurations();
			Configuration aotImplementation = configurations.getByName(aot.getImplementationConfigurationName());
			aotImplementation.extendsFrom(configurations.getByName(main.getImplementationConfigurationName()));
			aotImplementation.extendsFrom(configurations.getByName(main.getRuntimeOnlyConfigurationName()));
			configurations.getByName(aot.getCompileClasspathConfigurationName())
					.attributes((attributes) -> attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
							project.getObjects().named(LibraryElements.class, LibraryElements.CLASSES_AND_RESOURCES)));
		});
		return aotSourceSet;
	}

	private void registerGenerateAotSourcesTask(Project project, SourceSet aotSourceSet) {
		TaskProvider<ResolveMainClassName> resolveMainClassName = project.getTasks()
				.named(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class);
		TaskProvider<GenerateAotSources> generateAotSources = project.getTasks()
				.register(GENERATE_AOT_SOURCES_TASK_NAME, GenerateAotSources.class, (task) -> {
					Provider<Directory> generatedClasses = project.getLayout().getBuildDirectory()
							.dir(GENERATE_AOT_SOURCES_TASK_NAME);
					aotSourceSet.getOutput().dir(generatedClasses);
					task.getApplicationClass().set(resolveMainClassName.flatMap((thing) -> thing.readMainClassName()));
					task.setClasspath(aotSourceSet.getCompileClasspath());
					task.getSourcesDir().set(aotSourceSet.getJava().getSrcDirs().iterator().next());
					task.getResourcesDir().set(aotSourceSet.getResources().getSrcDirs().iterator().next());
					task.getClassesDir().set(generatedClasses);
					task.getGroupId().set(project.provider(() -> String.valueOf(project.getGroup())));
					task.getArtifactId().set(project.provider(() -> project.getName()));
				});
		project.getTasks().named(aotSourceSet.getCompileJavaTaskName())
				.configure((compileJava) -> compileJava.dependsOn(generateAotSources));
		project.getTasks().named(aotSourceSet.getProcessResourcesTaskName())
				.configure((processResources) -> processResources.dependsOn(generateAotSources));
	}

}
