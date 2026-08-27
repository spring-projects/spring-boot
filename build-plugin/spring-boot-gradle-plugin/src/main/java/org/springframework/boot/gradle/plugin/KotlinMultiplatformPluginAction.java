/*
 * Copyright 2012-present the original author or authors.
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

import java.util.Objects;
import java.util.concurrent.Callable;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension;
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation;
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget;

import org.springframework.boot.gradle.dsl.SpringBootExtension;
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage;
import org.springframework.boot.gradle.tasks.bundling.BootJar;
import org.springframework.boot.gradle.tasks.run.BootRun;
import org.springframework.util.Assert;

/**
 * {@link PluginApplicationAction} that is executed in response to the Kotlin
 * Multiplatform Plugin being applied.
 *
 * @author Somil Jain
 */
class KotlinMultiplatformPluginAction implements PluginApplicationAction {

	private final SinglePublishedArtifact singlePublishedArtifact;

	KotlinMultiplatformPluginAction(SinglePublishedArtifact singlePublishedArtifact) {
		this.singlePublishedArtifact = singlePublishedArtifact;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Plugin<? extends Project>> getPluginClass() throws ClassNotFoundException {
		return (Class<? extends Plugin<? extends Project>>) Class
			.forName("org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper");
	}

	@Override
	public void execute(Project project) {
		Configuration developmentOnly = configureDevelopmentOnlyConfiguration(project);
		Configuration testAndDevelopmentOnly = configureTestAndDevelopmentOnlyConfiguration(project);
		Configuration productionRuntimeClasspath = configureProductionRuntimeClasspathConfiguration(project);

		TaskProvider<ResolveMainClassName> resolveMainClassName = configureResolveMainClassNameTask(project);
		TaskProvider<ResolveMainClassName> resolveMainTestClassName = configureResolveMainTestClassNameTask(project);

		TaskProvider<BootJar> bootJar = configureBootJarTask(project, resolveMainClassName);
		configureBootBuildImageTask(project, bootJar);
		configureArtifactPublication(bootJar);

		TaskProvider<BootRun> bootRun = configureBootRunTask(project, resolveMainClassName);
		TaskProvider<BootRun> bootTestRun = configureBootTestRunTask(project, resolveMainTestClassName);

		configureToolchainConvention(project, bootRun);
		configureToolchainConvention(project, bootTestRun);

		configureBuildTask(project);

		KotlinMultiplatformExtension kotlinExtension = project.getExtensions()
			.getByType(KotlinMultiplatformExtension.class);

		kotlinExtension.getTargets()
			.withType(KotlinJvmTarget.class)
			.matching((target) -> "jvm".equals(target.getName()))
			.configureEach((jvmTarget) -> bindJvmTarget(project, jvmTarget, developmentOnly, testAndDevelopmentOnly,
					productionRuntimeClasspath, resolveMainClassName, resolveMainTestClassName, bootJar, bootRun,
					bootTestRun));
	}

	private void bindJvmTarget(Project project, KotlinJvmTarget jvmTarget, Configuration developmentOnly,
			Configuration testAndDevelopmentOnly, Configuration productionRuntimeClasspath,
			TaskProvider<ResolveMainClassName> resolveMainClassName,
			TaskProvider<ResolveMainClassName> resolveMainTestClassName, TaskProvider<BootJar> bootJar,
			TaskProvider<BootRun> bootRun, TaskProvider<BootRun> bootTestRun) {

		KotlinCompilation<?> mainCompilation = jvmTarget.getCompilations()
			.getByName(KotlinCompilation.MAIN_COMPILATION_NAME);
		KotlinCompilation<?> testCompilation = jvmTarget.getCompilations()
			.getByName(KotlinCompilation.TEST_COMPILATION_NAME);

		String runtimeConfigName = mainCompilation.getRuntimeDependencyConfigurationName();
		Assert.notNull(runtimeConfigName, "Runtime configuration name must not be null");
		Configuration runtimeClasspath = project.getConfigurations().getByName(runtimeConfigName);

		String testRuntimeConfigName = testCompilation.getRuntimeDependencyConfigurationName();
		Assert.notNull(testRuntimeConfigName, "Test runtime configuration name must not be null");
		Configuration testRuntimeClasspath = project.getConfigurations().getByName(testRuntimeConfigName);

		runtimeClasspath.extendsFrom(developmentOnly);
		runtimeClasspath.extendsFrom(testAndDevelopmentOnly);

		String implConfigName = testCompilation.getDefaultSourceSet().getImplementationConfigurationName();
		Assert.notNull(implConfigName, "Implementation configuration name must not be null");
		project.getConfigurations().getByName(implConfigName).extendsFrom(testAndDevelopmentOnly);

		bindProductionRuntimeClasspath(project, productionRuntimeClasspath, runtimeClasspath);

		String targetName = jvmTarget.getName();
		String runTaskPrefix = "run" + Character.toUpperCase(targetName.charAt(0)) + targetName.substring(1);
		Provider<String> kmpExecutableMainClass = project.provider(() -> project.getTasks()
			.withType(JavaExec.class)
			.matching((task) -> task.getName().startsWith(runTaskPrefix))
			.stream()
			.map((task) -> task.getMainClass().getOrNull())
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null));

		resolveMainClassName.configure((task) -> {
			task.setClasspath((Callable<FileCollection>) () -> mainCompilation.getOutput().getAllOutputs());
			task.getConfiguredMainClassName().convention(project.provider(() -> {
				SpringBootExtension springBootExtension = project.getExtensions().findByType(SpringBootExtension.class);
				String springBootMainClass = (springBootExtension != null)
						? springBootExtension.getMainClass().getOrNull() : null;
				if (springBootMainClass != null) {
					return springBootMainClass;
				}
				return kmpExecutableMainClass.getOrNull();
			}));
		});

		resolveMainTestClassName.configure((task) -> task.setClasspath((Callable<FileCollection>) () -> project
			.files(testCompilation.getOutput().getAllOutputs(), mainCompilation.getOutput().getAllOutputs())));

		bootJar.configure((task) -> {
			Callable<FileCollection> classpath = () -> project
				.files(mainCompilation.getOutput().getAllOutputs(), runtimeClasspath)
				.minus((developmentOnly.minus(productionRuntimeClasspath)))
				.minus((testAndDevelopmentOnly.minus(productionRuntimeClasspath)))
				.filter(new JarTypeFileSpec());

			task.classpath(classpath);
			task.getTargetJavaVersion()
				.set(project.provider(
						() -> JavaVersion.toVersion(jvmTarget.getCompilerOptions().getJvmTarget().get().getTarget())));
			task.resolvedArtifacts(runtimeClasspath.getIncoming().getArtifacts().getResolvedArtifacts());
		});

		bootRun.configure((task) -> {
			Callable<FileCollection> cp = () -> project
				.files(mainCompilation.getOutput().getAllOutputs(), runtimeClasspath)
				.filter(new JarTypeFileSpec());

			task.classpath(cp);
		});

		bootTestRun.configure((task) -> {
			Callable<FileCollection> cp = () -> project
				.files(testCompilation.getOutput().getAllOutputs(), mainCompilation.getOutput().getAllOutputs(),
						testRuntimeClasspath)
				.filter(new JarTypeFileSpec());

			task.classpath(cp);
		});

		project.getTasks()
			.named(jvmTarget.getArtifactsTaskName(), Jar.class)
			.configure((task) -> task.getArchiveClassifier().convention("plain"));
	}

	private void configureToolchainConvention(Project project, TaskProvider<BootRun> bootRun) {
		JavaPluginExtension javaPluginExtension = project.getExtensions().findByType(JavaPluginExtension.class);
		if (javaPluginExtension != null) {
			JavaToolchainService toolchains = project.getExtensions().findByType(JavaToolchainService.class);
			if (toolchains != null) {
				bootRun.configure((run) -> run.getJavaLauncher()
					.convention(toolchains.launcherFor(javaPluginExtension.getToolchain())));
			}
		}
	}

	private void configureBuildTask(Project project) {
		project.getTasks()
			.named(BasePlugin.ASSEMBLE_TASK_NAME)
			.configure((task) -> task.dependsOn(this.singlePublishedArtifact));
	}

	private TaskProvider<ResolveMainClassName> configureResolveMainClassNameTask(Project project) {
		return project.getTasks()
			.register(SpringBootPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class, (task) -> {
				task.setDescription("Resolves the name of the application's main class.");
				task.setGroup(BasePlugin.BUILD_GROUP);
				task.getOutputFile().set(project.getLayout().getBuildDirectory().file("resolvedMainClassName"));
			});
	}

	private TaskProvider<ResolveMainClassName> configureResolveMainTestClassNameTask(Project project) {
		return project.getTasks()
			.register(SpringBootPlugin.RESOLVE_TEST_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class, (task) -> {
				task.setDescription("Resolves the name of the application's test main class.");
				task.setGroup(BasePlugin.BUILD_GROUP);
				task.getOutputFile().set(project.getLayout().getBuildDirectory().file("resolvedMainTestClassName"));
			});
	}

	private TaskProvider<BootJar> configureBootJarTask(Project project,
			TaskProvider<ResolveMainClassName> resolveMainClassName) {
		return project.getTasks().register(SpringBootPlugin.BOOT_JAR_TASK_NAME, BootJar.class, (bootJar) -> {
			bootJar.setDescription(
					"Assembles an executable jar archive containing the main classes and their dependencies.");
			bootJar.setGroup(BasePlugin.BUILD_GROUP);

			Provider<String> manifestStartClass = project
				.provider(() -> (String) bootJar.getManifest().getAttributes().get("Start-Class"));
			bootJar.getMainClass()
				.convention(resolveMainClassName.flatMap((resolver) -> manifestStartClass.isPresent()
						? manifestStartClass : resolver.readMainClassName()));
		});
	}

	private void configureBootBuildImageTask(Project project, TaskProvider<BootJar> bootJar) {
		project.getTasks().register(SpringBootPlugin.BOOT_BUILD_IMAGE_TASK_NAME, BootBuildImage.class, (buildImage) -> {
			buildImage.setDescription("Builds an OCI image of the application using the output of the bootJar task");
			buildImage.setGroup(BasePlugin.BUILD_GROUP);
			buildImage.getArchiveFile().set(bootJar.get().getArchiveFile());
		});
	}

	private void configureArtifactPublication(TaskProvider<BootJar> bootJar) {
		this.singlePublishedArtifact.addJarCandidate(bootJar);
	}

	private TaskProvider<BootRun> configureBootRunTask(Project project,
			TaskProvider<ResolveMainClassName> resolveMainClassName) {
		return project.getTasks().register(SpringBootPlugin.BOOT_RUN_TASK_NAME, BootRun.class, (run) -> {
			run.setDescription("Runs this project as a Spring Boot application.");
			run.setGroup(ApplicationPlugin.APPLICATION_GROUP);
			run.getMainClass().convention(resolveMainClassName.flatMap(ResolveMainClassName::readMainClassName));
		});
	}

	private TaskProvider<BootRun> configureBootTestRunTask(Project project,
			TaskProvider<ResolveMainClassName> resolveMainClassName) {
		return project.getTasks().register("bootTestRun", BootRun.class, (run) -> {
			run.setDescription("Runs this project as a Spring Boot application using the test runtime classpath.");
			run.setGroup(ApplicationPlugin.APPLICATION_GROUP);
			run.getMainClass().convention(resolveMainClassName.flatMap(ResolveMainClassName::readMainClassName));
		});
	}

	private Configuration configureProductionRuntimeClasspathConfiguration(Project project) {
		return project.getConfigurations().create(SpringBootPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "UnstableApiUsage" })
	private void bindProductionRuntimeClasspath(Project project, Configuration productionRuntimeClasspath,
			Configuration runtimeClasspath) {
		productionRuntimeClasspath.attributes((attributes) -> {
			ProviderFactory providers = project.getProviders();
			AttributeContainer sourceAttributes = runtimeClasspath.getAttributes();
			for (Attribute attribute : sourceAttributes.keySet()) {
				attributes.attributeProvider(attribute,
						providers.provider(() -> sourceAttributes.getAttribute(attribute)));
			}
		});
		productionRuntimeClasspath.setExtendsFrom(runtimeClasspath.getExtendsFrom());
		productionRuntimeClasspath.setCanBeResolved(runtimeClasspath.isCanBeResolved());
		productionRuntimeClasspath.setCanBeConsumed(runtimeClasspath.isCanBeConsumed());
		productionRuntimeClasspath.shouldResolveConsistentlyWith(runtimeClasspath);
	}

	private Configuration configureDevelopmentOnlyConfiguration(Project project) {
		Configuration developmentOnly = project.getConfigurations()
			.create(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		developmentOnly
			.setDescription("Configuration for development-only dependencies such as Spring Boot's DevTools.");
		return developmentOnly;
	}

	private Configuration configureTestAndDevelopmentOnlyConfiguration(Project project) {
		Configuration testAndDevelopmentOnly = project.getConfigurations()
			.create(SpringBootPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		testAndDevelopmentOnly
			.setDescription("Configuration for test and development-only dependencies such as Spring Boot's DevTools.");
		return testAndDevelopmentOnly;
	}

}
