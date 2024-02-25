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

import java.util.Set;
import java.util.stream.Stream;

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
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

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

	/**
	 * Applies the SpringBootAotPlugin to the given project. This plugin is responsible
	 * for configuring the project's source sets and registering AOT tasks.
	 * @param project The project to apply the plugin to.
	 */
	@Override
	public void apply(Project project) {
		PluginContainer plugins = project.getPlugins();
		plugins.withType(JavaPlugin.class).all((javaPlugin) -> {
			JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
			SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
			SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			SourceSet aotSourceSet = configureSourceSet(project, AOT_SOURCE_SET_NAME, mainSourceSet);
			SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
			SourceSet aotTestSourceSet = configureSourceSet(project, AOT_TEST_SOURCE_SET_NAME, testSourceSet);
			plugins.withType(SpringBootPlugin.class).all((bootPlugin) -> {
				registerProcessAotTask(project, aotSourceSet, mainSourceSet);
				registerProcessTestAotTask(project, mainSourceSet, aotTestSourceSet, testSourceSet);
			});
		});
	}

	/**
	 * Configures a new source set with the given name and existing source set.
	 * @param project The project to configure the source set for.
	 * @param newSourceSetName The name of the new source set to create.
	 * @param existingSourceSet The existing source set to base the new source set on.
	 * @return The configured source set.
	 */
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

	/**
	 * Configures the classes and resources library elements attribute for the given
	 * project and attribute container.
	 * @param project the project to configure
	 * @param attributes the attribute container to configure
	 */
	private void configureClassesAndResourcesLibraryElementsAttribute(Project project, AttributeContainer attributes) {
		LibraryElements classesAndResources = project.getObjects()
			.named(LibraryElements.class, LibraryElements.CLASSES_AND_RESOURCES);
		attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, classesAndResources);
	}

	/**
	 * Configures the usage attribute for the Java runtime in the given project.
	 * @param project the project to configure
	 * @param attributes the attribute container to add the usage attribute to
	 */
	private void configureJavaRuntimeUsageAttribute(Project project, AttributeContainer attributes) {
		Usage javaRuntime = project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME);
		attributes.attribute(Usage.USAGE_ATTRIBUTE, javaRuntime);
	}

	/**
	 * Registers the processAot task for the AOT source set in the SpringBootAotPlugin
	 * class.
	 * @param project The project object.
	 * @param aotSourceSet The AOT source set.
	 * @param mainSourceSet The main source set.
	 */
	private void registerProcessAotTask(Project project, SourceSet aotSourceSet, SourceSet mainSourceSet) {
		TaskProvider<ResolveMainClassName> resolveMainClassName = project.getTasks()
			.named(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class);
		Configuration aotClasspath = createAotProcessingClasspath(project, PROCESS_AOT_TASK_NAME, mainSourceSet,
				Set.of(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME,
						SpringBootPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME));
		project.getDependencies().add(aotClasspath.getName(), project.files(mainSourceSet.getOutput()));
		Configuration compileClasspath = project.getConfigurations()
			.getByName(aotSourceSet.getCompileClasspathConfigurationName());
		compileClasspath.extendsFrom(aotClasspath);
		Provider<Directory> resourcesOutput = project.getLayout()
			.getBuildDirectory()
			.dir("generated/" + aotSourceSet.getName() + "Resources");
		TaskProvider<ProcessAot> processAot = project.getTasks()
			.register(PROCESS_AOT_TASK_NAME, ProcessAot.class, (task) -> {
				configureAotTask(project, aotSourceSet, task, resourcesOutput);
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

	/**
	 * Configures the Ahead-of-Time (AOT) task for the given project, source set, and
	 * task.
	 * @param project The project to configure the AOT task for.
	 * @param sourceSet The source set to configure the AOT task for.
	 * @param task The AOT task to configure.
	 * @param resourcesOutput The output directory for the resources.
	 */
	private void configureAotTask(Project project, SourceSet sourceSet, AbstractAot task,
			Provider<Directory> resourcesOutput) {
		task.getSourcesOutput()
			.set(project.getLayout().getBuildDirectory().dir("generated/" + sourceSet.getName() + "Sources"));
		task.getResourcesOutput().set(resourcesOutput);
		task.getClassesOutput()
			.set(project.getLayout().getBuildDirectory().dir("generated/" + sourceSet.getName() + "Classes"));
		task.getGroupId().set(project.provider(() -> String.valueOf(project.getGroup())));
		task.getArtifactId().set(project.provider(() -> project.getName()));
		configureToolchainConvention(project, task);
	}

	/**
	 * Configures the toolchain convention for the given project and AOT task.
	 * @param project the project to configure the toolchain for
	 * @param aotTask the AOT task to configure the toolchain for
	 */
	private void configureToolchainConvention(Project project, AbstractAot aotTask) {
		JavaToolchainSpec toolchain = project.getExtensions().getByType(JavaPluginExtension.class).getToolchain();
		JavaToolchainService toolchainService = project.getExtensions().getByType(JavaToolchainService.class);
		aotTask.getJavaLauncher().convention(toolchainService.launcherFor(toolchain));
	}

	/**
	 * Creates the AOT processing classpath configuration for the specified project, task
	 * name, input source set, and development only configuration names.
	 * @param project The project to create the configuration for.
	 * @param taskName The name of the task.
	 * @param inputSourceSet The input source set.
	 * @param developmentOnlyConfigurationNames The set of development only configuration
	 * names.
	 * @return The created AOT processing classpath configuration.
	 * @throws IllegalStateException If the classpath cannot be resolved.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Configuration createAotProcessingClasspath(Project project, String taskName, SourceSet inputSourceSet,
			Set<String> developmentOnlyConfigurationNames) {
		Configuration base = project.getConfigurations()
			.getByName(inputSourceSet.getRuntimeClasspathConfigurationName());
		return project.getConfigurations().create(taskName + "Classpath", (classpath) -> {
			classpath.setCanBeConsumed(false);
			if (!classpath.isCanBeResolved()) {
				throw new IllegalStateException("Unexpected");
			}
			classpath.setCanBeResolved(true);
			classpath.setDescription("Classpath of the " + taskName + " task.");
			removeDevelopmentOnly(base.getExtendsFrom(), developmentOnlyConfigurationNames)
				.forEach(classpath::extendsFrom);
			classpath.attributes((attributes) -> {
				ProviderFactory providers = project.getProviders();
				AttributeContainer baseAttributes = base.getAttributes();
				for (Attribute attribute : baseAttributes.keySet()) {
					attributes.attributeProvider(attribute,
							providers.provider(() -> baseAttributes.getAttribute(attribute)));
				}
			});
		});
	}

	/**
	 * Removes development-only configurations from the given set of configurations.
	 * @param configurations the set of configurations to filter
	 * @param developmentOnlyConfigurationNames the set of development-only configuration
	 * names
	 * @return a stream of configurations without development-only configurations
	 */
	private Stream<Configuration> removeDevelopmentOnly(Set<Configuration> configurations,
			Set<String> developmentOnlyConfigurationNames) {
		return configurations.stream()
			.filter((configuration) -> !developmentOnlyConfigurationNames.contains(configuration.getName()));
	}

	/**
	 * Configures the dependency between the processResources task of the given source set
	 * and the provided processAot task.
	 * @param project The project to configure.
	 * @param aotSourceSet The source set to configure.
	 * @param processAot The task provider for the processAot task.
	 */
	private void configureDependsOn(Project project, SourceSet aotSourceSet,
			TaskProvider<? extends AbstractAot> processAot) {
		project.getTasks()
			.named(aotSourceSet.getProcessResourcesTaskName())
			.configure((processResources) -> processResources.dependsOn(processAot));
	}

	/**
	 * Registers the processTestAot task for AOT processing in the SpringBootAotPlugin
	 * class.
	 * @param project The project object representing the current Gradle project.
	 * @param mainSourceSet The main source set of the project.
	 * @param aotTestSourceSet The AOT test source set of the project.
	 * @param testSourceSet The test source set of the project.
	 */
	private void registerProcessTestAotTask(Project project, SourceSet mainSourceSet, SourceSet aotTestSourceSet,
			SourceSet testSourceSet) {
		Configuration aotClasspath = createAotProcessingClasspath(project, PROCESS_TEST_AOT_TASK_NAME, testSourceSet,
				Set.of(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME));
		addJUnitPlatformLauncherDependency(project, aotClasspath);
		Configuration compileClasspath = project.getConfigurations()
			.getByName(aotTestSourceSet.getCompileClasspathConfigurationName());
		compileClasspath.extendsFrom(aotClasspath);
		Provider<Directory> resourcesOutput = project.getLayout()
			.getBuildDirectory()
			.dir("generated/" + aotTestSourceSet.getName() + "Resources");
		TaskProvider<ProcessTestAot> processTestAot = project.getTasks()
			.register(PROCESS_TEST_AOT_TASK_NAME, ProcessTestAot.class, (task) -> {
				configureAotTask(project, aotTestSourceSet, task, resourcesOutput);
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

	/**
	 * Adds the JUnit Platform Launcher dependency to the specified project and
	 * configuration.
	 * @param project the project to add the dependency to
	 * @param configuration the configuration to add the dependency to
	 */
	private void addJUnitPlatformLauncherDependency(Project project, Configuration configuration) {
		DependencyHandler dependencyHandler = project.getDependencies();
		Dependency springBootDependencies = dependencyHandler
			.create(dependencyHandler.platform(SpringBootPlugin.BOM_COORDINATES));
		DependencySet dependencies = configuration.getDependencies();
		dependencies.add(springBootDependencies);
		dependencies.add(dependencyHandler.create("org.junit.platform:junit-platform-launcher"));
	}

}
