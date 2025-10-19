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

package org.springframework.boot.build;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration;
import com.gradle.develocity.agent.gradle.test.PredictiveTestSelectionConfiguration;
import com.gradle.develocity.agent.gradle.test.TestRetryConfiguration;
import io.spring.gradle.nullability.NullabilityPlugin;
import io.spring.gradle.nullability.NullabilityPluginExtension;
import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import io.spring.javaformat.gradle.tasks.CheckFormat;
import io.spring.javaformat.gradle.tasks.Format;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.external.javadoc.CoreJavadocOptions;

import org.springframework.boot.build.architecture.ArchitecturePlugin;
import org.springframework.boot.build.classpath.CheckClasspathForProhibitedDependencies;
import org.springframework.boot.build.optional.OptionalDependenciesPlugin;
import org.springframework.boot.build.springframework.CheckAotFactories;
import org.springframework.boot.build.springframework.CheckSpringFactories;
import org.springframework.boot.build.testing.TestFailuresPlugin;
import org.springframework.boot.build.toolchain.ToolchainPlugin;
import org.springframework.util.StringUtils;

/**
 * Conventions that are applied in the presence of the {@link JavaBasePlugin}. When the
 * plugin is applied:
 *
 * <ul>
 * <li>The project is configured with source and target compatibility of 17
 * <li>{@link SpringJavaFormatPlugin Spring Java Format}, {@link CheckstylePlugin
 * Checkstyle}, {@link TestFailuresPlugin Test Failures}, {@link ArchitecturePlugin
 * Architecture} and {@link NullabilityPlugin} plugins are applied
 * <li>{@link Test} tasks are configured:
 * <ul>
 * <li>to use JUnit Platform
 * <li>with a max heap of 1536M
 * <li>to run after any Checkstyle and format checking tasks
 * <li>to enable retries with a maximum of three attempts when running on CI
 * <li>to use predictive test selection when the value of the
 * {@code ENABLE_PREDICTIVE_TEST_SELECTION} environment variable is {@code true}
 * </ul>
 * <li>A {@code testRuntimeOnly} dependency upon
 * {@code org.junit.platform:junit-platform-launcher} is added to projects with the
 * {@link JavaPlugin} applied
 * <li>{@link JavaCompile}, {@link Javadoc}, and {@link Format} tasks are configured to
 * use UTF-8 encoding
 * <li>{@link JavaCompile} tasks are configured to:
 * <ul>
 * <li>Use {@code -parameters}.
 * <li>Treat warnings as errors
 * <li>Enable {@code unchecked}, {@code deprecation}, {@code rawtypes}, and
 * {@code varargs} warnings
 * </ul>
 * <li>{@link Jar} tasks are configured to produce jars with LICENSE.txt and NOTICE.txt
 * files and the following manifest entries:
 * <ul>
 * <li>{@code Automatic-Module-Name}
 * <li>{@code Build-Jdk-Spec}
 * <li>{@code Built-By}
 * <li>{@code Implementation-Title}
 * <li>{@code Implementation-Version}
 * </ul>
 * <li>{@code spring-boot-parent} is used for dependency management</li>
 * <li>Additional checks are configured:
 * <ul>
 * <li>For all source sets:
 * <ul>
 * <li>Prohibited dependencies on the compile classpath
 * <li>Prohibited dependencies on the runtime classpath
 * </ul>
 * <li>For the {@code main} source set:
 * <ul>
 * <li>{@code META-INF/spring/aot.factories}
 * <li>{@code META-INF/spring.factories}
 * </ul>
 * </ul>
 * </ul>
 *
 * <p/>
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 * @author Scott Frederick
 */
class JavaConventions {

	private static final String SOURCE_AND_TARGET_COMPATIBILITY = "17";

	void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> {
			project.getPlugins().apply(TestFailuresPlugin.class);
			project.getPlugins().apply(ArchitecturePlugin.class);
			configureSpringJavaFormat(project);
			configureJavaConventions(project);
			configureJavadocConventions(project);
			configureTestConventions(project);
			configureJarManifestConventions(project);
			configureDependencyManagement(project);
			configureToolchain(project);
			configureProhibitedDependencyChecks(project);
			configureFactoriesFilesChecks(project);
			configureNullability(project);
		});
	}

	private void configureJarManifestConventions(Project project) {
		TaskProvider<ExtractResources> extractLegalResources = project.getTasks()
			.register("extractLegalResources", ExtractResources.class, (task) -> {
				task.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("legal"));
				task.getResourceNames().set(Arrays.asList("LICENSE.txt", "NOTICE.txt"));
				task.getProperties().put("version", project.getVersion().toString());
			});
		SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
		Set<String> sourceJarTaskNames = sourceSets.stream()
			.map(SourceSet::getSourcesJarTaskName)
			.collect(Collectors.toSet());
		Set<String> javadocJarTaskNames = sourceSets.stream()
			.map(SourceSet::getJavadocJarTaskName)
			.collect(Collectors.toSet());
		project.getTasks().withType(Jar.class, (jar) -> project.afterEvaluate((evaluated) -> {
			jar.metaInf((metaInf) -> metaInf.from(extractLegalResources));
			jar.manifest((manifest) -> {
				Map<String, Object> attributes = new TreeMap<>();
				attributes.put("Automatic-Module-Name", project.getName().replace("-", "."));
				attributes.put("Build-Jdk-Spec", SOURCE_AND_TARGET_COMPATIBILITY);
				attributes.put("Built-By", "Spring");
				attributes.put("Implementation-Title",
						determineImplementationTitle(project, sourceJarTaskNames, javadocJarTaskNames, jar));
				attributes.put("Implementation-Version", project.getVersion());
				manifest.attributes(attributes);
			});
		}));
	}

	private String determineImplementationTitle(Project project, Set<String> sourceJarTaskNames,
			Set<String> javadocJarTaskNames, Jar jar) {
		if (sourceJarTaskNames.contains(jar.getName())) {
			return "Source for " + project.getName();
		}
		if (javadocJarTaskNames.contains(jar.getName())) {
			return "Javadoc for " + project.getName();
		}
		return project.getDescription();
	}

	private void configureTestConventions(Project project) {
		project.getTasks().withType(Test.class, (test) -> {
			test.useJUnitPlatform();
			test.setMaxHeapSize("1536M");
			project.getTasks().withType(Checkstyle.class, test::mustRunAfter);
			project.getTasks().withType(CheckFormat.class, test::mustRunAfter);
			configureTestRetries(test);
			configurePredictiveTestSelection(test);
		});
		project.getPlugins()
			.withType(JavaPlugin.class, (javaPlugin) -> project.getDependencies()
				.add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.platform:junit-platform-launcher"));
	}

	private void configureTestRetries(Test test) {
		TestRetryConfiguration testRetry = test.getExtensions()
			.getByType(DevelocityTestConfiguration.class)
			.getTestRetry();
		testRetry.getFailOnPassedAfterRetry().set(false);
		testRetry.getMaxRetries().set(isCi() ? 3 : 0);
	}

	private boolean isCi() {
		return Boolean.parseBoolean(System.getenv("CI"));
	}

	private void configurePredictiveTestSelection(Test test) {
		if (isPredictiveTestSelectionEnabled()) {
			PredictiveTestSelectionConfiguration predictiveTestSelection = test.getExtensions()
				.getByType(DevelocityTestConfiguration.class)
				.getPredictiveTestSelection();
			predictiveTestSelection.getEnabled().convention(true);
		}
	}

	private boolean isPredictiveTestSelectionEnabled() {
		return Boolean.parseBoolean(System.getenv("ENABLE_PREDICTIVE_TEST_SELECTION"));
	}

	private void configureJavadocConventions(Project project) {
		project.getTasks().withType(Javadoc.class, (javadoc) -> {
			CoreJavadocOptions options = (CoreJavadocOptions) javadoc.getOptions();
			options.source("17");
			options.encoding("UTF-8");
			options.addStringOption("Xdoclint:none", "-quiet");
		});
	}

	private void configureJavaConventions(Project project) {
		if (!project.hasProperty("toolchainVersion")) {
			JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
			javaPluginExtension.setSourceCompatibility(JavaVersion.toVersion(SOURCE_AND_TARGET_COMPATIBILITY));
			javaPluginExtension.setTargetCompatibility(JavaVersion.toVersion(SOURCE_AND_TARGET_COMPATIBILITY));
		}
		project.getTasks().withType(JavaCompile.class, (compile) -> {
			compile.getOptions().setEncoding("UTF-8");
			compile.getOptions().getRelease().set(17);
			List<String> args = compile.getOptions().getCompilerArgs();
			if (!args.contains("-parameters")) {
				args.add("-parameters");
			}
			args.addAll(Arrays.asList("-Werror", "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:rawtypes",
					"-Xlint:varargs"));
		});
	}

	private void configureSpringJavaFormat(Project project) {
		project.getPlugins().apply(SpringJavaFormatPlugin.class);
		project.getTasks().withType(Format.class, (Format) -> Format.setEncoding("UTF-8"));
		project.getPlugins().apply(CheckstylePlugin.class);
		CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
		String checkstyleToolVersion = (String) project.findProperty("checkstyleToolVersion");
		checkstyle.setToolVersion(checkstyleToolVersion);
		checkstyle.getConfigDirectory().set(project.getRootProject().file("config/checkstyle"));
		String version = SpringJavaFormatPlugin.class.getPackage().getImplementationVersion();
		DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
		checkstyleDependencies
			.add(project.getDependencies().create("com.puppycrawl.tools:checkstyle:" + checkstyle.getToolVersion()));
		checkstyleDependencies
			.add(project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:" + version));
	}

	private void configureDependencyManagement(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		Configuration dependencyManagement = configurations.create("dependencyManagement", (configuration) -> {
			configuration.setVisible(false);
			configuration.setCanBeConsumed(false);
			configuration.setCanBeResolved(false);
		});
		configurations
			.matching((configuration) -> (configuration.getName().endsWith("Classpath")
					|| JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME.equals(configuration.getName()))
					&& (!configuration.getName().contains("dokka")))
			.all((configuration) -> configuration.extendsFrom(dependencyManagement));
		Dependency springBootParent = project.getDependencies()
			.enforcedPlatform(project.getDependencies()
				.project(Collections.singletonMap("path", ":platform:spring-boot-internal-dependencies")));
		dependencyManagement.getDependencies().add(springBootParent);
		project.getPlugins()
			.withType(OptionalDependenciesPlugin.class,
					(optionalDependencies) -> configurations
						.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME)
						.extendsFrom(dependencyManagement));
	}

	private void configureToolchain(Project project) {
		project.getPlugins().apply(ToolchainPlugin.class);
	}

	private void configureProhibitedDependencyChecks(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
		sourceSets.all((sourceSet) -> createProhibitedDependenciesChecks(project,
				sourceSet.getCompileClasspathConfigurationName(), sourceSet.getRuntimeClasspathConfigurationName()));
	}

	private void createProhibitedDependenciesChecks(Project project, String... configurationNames) {
		ConfigurationContainer configurations = project.getConfigurations();
		for (String configurationName : configurationNames) {
			Configuration configuration = configurations.getByName(configurationName);
			createProhibitedDependenciesCheck(configuration, project);
		}
	}

	private void createProhibitedDependenciesCheck(Configuration classpath, Project project) {
		TaskProvider<CheckClasspathForProhibitedDependencies> checkClasspathForProhibitedDependencies = project
			.getTasks()
			.register("check" + StringUtils.capitalize(classpath.getName() + "ForProhibitedDependencies"),
					CheckClasspathForProhibitedDependencies.class, (task) -> task.setClasspath(classpath));
		project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(checkClasspathForProhibitedDependencies);
	}

	private void configureFactoriesFilesChecks(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
		sourceSets.matching((sourceSet) -> SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName()))
			.configureEach((main) -> {
				TaskProvider<Task> check = project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME);
				TaskProvider<CheckAotFactories> checkAotFactories = project.getTasks()
					.register("checkAotFactories", CheckAotFactories.class, (task) -> {
						task.setSource(main.getResources());
						task.setClasspath(main.getOutput().getClassesDirs());
						task.setDescription("Checks the META-INF/spring/aot.factories file of the main source set.");
					});
				check.configure((task) -> task.dependsOn(checkAotFactories));
				TaskProvider<CheckSpringFactories> checkSpringFactories = project.getTasks()
					.register("checkSpringFactories", CheckSpringFactories.class, (task) -> {
						task.setSource(main.getResources());
						task.setClasspath(main.getOutput().getClassesDirs());
						task.setDescription("Checks the META-INF/spring.factories file of the main source set.");
					});
				check.configure((task) -> task.dependsOn(checkSpringFactories));
			});
	}

	private void configureNullability(Project project) {
		project.getPlugins().apply(NullabilityPlugin.class);
		NullabilityPluginExtension extension = project.getExtensions().getByType(NullabilityPluginExtension.class);
		String nullAwayVersion = (String) project.findProperty("nullAwayVersion");
		if (nullAwayVersion != null) {
			extension.getNullAwayVersion().set(nullAwayVersion);
		}
		String errorProneVersion = (String) project.findProperty("errorProneVersion");
		if (errorProneVersion != null) {
			extension.getErrorProneVersion().set(errorProneVersion);
		}
	}

}
