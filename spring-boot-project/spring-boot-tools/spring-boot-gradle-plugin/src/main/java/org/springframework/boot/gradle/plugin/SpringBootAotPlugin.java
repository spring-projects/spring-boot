/*
 * Copyright 2012-2023 the original author or authors.
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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ConfigurableFileCollection;
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
			JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
			SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
			SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			SourceSet aotSourceSet = configureSourceSet(project, "aot", mainSourceSet);
			SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
			SourceSet aotTestSourceSet = configureSourceSet(project, "aotTest", testSourceSet);
			plugins.withType(SpringBootPlugin.class).all((bootPlugin) -> {
				registerProcessAotTask(project, aotSourceSet, mainSourceSet);
				registerProcessTestAotTask(project, mainSourceSet, aotTestSourceSet, testSourceSet);
			});
		});
	}

	private SourceSet configureSourceSet(Project project, String newSourceSetName, SourceSet existingSourceSet) {
		JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
		return sourceSets.create(newSourceSetName, (sourceSet) -> {
			existingSourceSet.setRuntimeClasspath(existingSourceSet.getRuntimeClasspath().plus(sourceSet.getOutput()));
			project.getConfigurations()
				.getByName(sourceSet.getCompileClasspathConfigurationName())
				.attributes((attributes) -> {
					configureClassesAndResourcesLibraryElementsAttribute(project, attributes);
					configureJavaRuntimeUsageAttribute(project, attributes);
				});
		});
	}

	private void configureClassesAndResourcesLibraryElementsAttribute(Project project, AttributeContainer attributes) {
		LibraryElements classesAndResources = project.getObjects()
			.named(LibraryElements.class, LibraryElements.CLASSES_AND_RESOURCES);
		attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, classesAndResources);
	}

	private void configureJavaRuntimeUsageAttribute(Project project, AttributeContainer attributes) {
		Usage javaRuntime = project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME);
		attributes.attribute(Usage.USAGE_ATTRIBUTE, javaRuntime);
	}

	private void registerProcessAotTask(Project project, SourceSet aotSourceSet, SourceSet mainSourceSet) {
		TaskProvider<ResolveMainClassName> resolveMainClassName = project.getTasks()
			.named(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class);
		Configuration aotClasspath = createAotProcessingClasspath(project, PROCESS_AOT_TASK_NAME, mainSourceSet);
		project.getDependencies().add(aotClasspath.getName(), project.files(mainSourceSet.getOutput()));
		Configuration compileClasspath = project.getConfigurations()
			.getByName(aotSourceSet.getCompileClasspathConfigurationName());
		compileClasspath.extendsFrom(aotClasspath);
		Provider<Directory> resourcesOutput = project.getLayout()
			.getBuildDirectory()
			.dir("generated/" + aotSourceSet.getName() + "Resources");
		TaskProvider<ProcessAot> processAot = project.getTasks()
			.register(PROCESS_AOT_TASK_NAME, ProcessAot.class, (task) -> {
				configureAotTask(project, aotSourceSet, task, mainSourceSet, resourcesOutput);
				task.getApplicationMainClass()
					.set(resolveMainClassName.flatMap(ResolveMainClassName::readMainClassName));
				task.setClasspath(aotClasspath);
			});
		aotSourceSet.getJava().srcDir(processAot.map(ProcessAot::getSourcesOutput));
		aotSourceSet.getResources().srcDir(resourcesOutput);
		ConfigurableFileCollection classesOutputFiles = project.files(processAot.map(ProcessAot::getClassesOutput));
		mainSourceSet.setRuntimeClasspath(mainSourceSet.getRuntimeClasspath().plus(classesOutputFiles));
		project.getDependencies().add(aotSourceSet.getImplementationConfigurationName(), classesOutputFiles);
		configureDependsOn(project, aotSourceSet, processAot);
	}

	private void configureAotTask(Project project, SourceSet sourceSet, AbstractAot task, SourceSet inputSourceSet,
			Provider<Directory> resourcesOutput) {
		task.getSourcesOutput()
			.set(project.getLayout().getBuildDirectory().dir("generated/" + sourceSet.getName() + "Sources"));
		task.getResourcesOutput().set(resourcesOutput);
		task.getClassesOutput()
			.set(project.getLayout().getBuildDirectory().dir("generated/" + sourceSet.getName() + "Classes"));
		task.getGroupId().set(project.provider(() -> String.valueOf(project.getGroup())));
		task.getArtifactId().set(project.provider(() -> project.getName()));
	}

	@SuppressWarnings("unchecked")
	private Configuration createAotProcessingClasspath(Project project, String taskName, SourceSet inputSourceSet) {
		Configuration base = project.getConfigurations()
			.getByName(inputSourceSet.getRuntimeClasspathConfigurationName());
		Configuration aotClasspath = project.getConfigurations().create(taskName + "Classpath", (classpath) -> {
			classpath.setCanBeConsumed(false);
			classpath.setCanBeResolved(true);
			classpath.setDescription("Classpath of the " + taskName + " task.");
			base.getExtendsFrom().forEach(classpath::extendsFrom);
			classpath.attributes((attributes) -> {
				AttributeContainer baseAttributes = base.getAttributes();
				for (Attribute<?> attribute : baseAttributes.keySet()) {
					attributes.attribute((Attribute<Object>) attribute, baseAttributes.getAttribute(attribute));
				}
			});
		});
		return aotClasspath;
	}

	private void configureDependsOn(Project project, SourceSet aotSourceSet,
			TaskProvider<? extends AbstractAot> processAot) {
		project.getTasks()
			.named(aotSourceSet.getProcessResourcesTaskName())
			.configure((processResources) -> processResources.dependsOn(processAot));
	}

	private void registerProcessTestAotTask(Project project, SourceSet mainSourceSet, SourceSet aotTestSourceSet,
			SourceSet testSourceSet) {
		Configuration aotClasspath = createAotProcessingClasspath(project, PROCESS_TEST_AOT_TASK_NAME, testSourceSet);
		addJUnitPlatformLauncherDependency(project, aotClasspath);
		Configuration compileClasspath = project.getConfigurations()
			.getByName(aotTestSourceSet.getCompileClasspathConfigurationName());
		compileClasspath.extendsFrom(aotClasspath);
		Provider<Directory> resourcesOutput = project.getLayout()
			.getBuildDirectory()
			.dir("generated/" + aotTestSourceSet.getName() + "Resources");
		TaskProvider<ProcessTestAot> processTestAot = project.getTasks()
			.register(PROCESS_TEST_AOT_TASK_NAME, ProcessTestAot.class, (task) -> {
				configureAotTask(project, aotTestSourceSet, task, testSourceSet, resourcesOutput);
				task.setClasspath(aotClasspath);
				task.setClasspathRoots(testSourceSet.getOutput());
			});
		aotTestSourceSet.getJava().srcDir(processTestAot.map(ProcessTestAot::getSourcesOutput));
		aotTestSourceSet.getResources().srcDir(resourcesOutput);
		project.getDependencies().add(aotClasspath.getName(), project.files(mainSourceSet.getOutput()));
		project.getDependencies().add(aotClasspath.getName(), project.files(testSourceSet.getOutput()));
		ConfigurableFileCollection classesOutputFiles = project
			.files(processTestAot.map(ProcessTestAot::getClassesOutput));
		testSourceSet.setRuntimeClasspath(testSourceSet.getRuntimeClasspath().plus(classesOutputFiles));
		project.getDependencies().add(aotTestSourceSet.getImplementationConfigurationName(), classesOutputFiles);
		configureDependsOn(project, aotTestSourceSet, processTestAot);
	}

	private void addJUnitPlatformLauncherDependency(Project project, Configuration configuration) {
		DependencyHandler dependencyHandler = project.getDependencies();
		Dependency springBootDependencies = dependencyHandler
			.create(dependencyHandler.platform(SpringBootPlugin.BOM_COORDINATES));
		DependencySet dependencies = configuration.getDependencies();
		dependencies.add(springBootDependencies);
		dependencies.add(dependencyHandler.create("org.junit.platform:junit-platform-launcher"));
	}

}
