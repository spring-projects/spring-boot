/*
 * Copyright 2023-2024 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gradle.node.NodeExtension;
import com.github.gradle.node.npm.task.NpmInstallTask;
import io.spring.gradle.antora.GenerateAntoraYmlPlugin;
import io.spring.gradle.antora.GenerateAntoraYmlTask;
import org.antora.gradle.AntoraPlugin;
import org.antora.gradle.AntoraTask;
import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskContainer;

import org.springframework.boot.build.antora.AntoraAsciidocAttributes;
import org.springframework.boot.build.antora.GenerateAntoraPlaybook;
import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.constraints.ExtractVersionConstraints;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Conventions that are applied in the presence of the {@link AntoraPlugin}.
 *
 * @author Phillip Webb
 */
public class AntoraConventions {

	private static final String DEPENDENCIES_PATH = ":spring-boot-project:spring-boot-dependencies";

	private static final String ANTORA_SOURCE_DIR = "src/docs/antora";

	private static final List<String> NAV_FILES = List.of("nav.adoc", "local-nav.adoc");

	void apply(Project project) {
		project.getPlugins().withType(AntoraPlugin.class, (antoraPlugin) -> apply(project, antoraPlugin));
	}

	private void apply(Project project, AntoraPlugin antoraPlugin) {
		ExtractVersionConstraints dependencyVersionsTask = addDependencyVersionsTask(project);
		project.getPlugins().apply(GenerateAntoraYmlPlugin.class);
		TaskContainer tasks = project.getTasks();
		GenerateAntoraPlaybook generateAntoraPlaybookTask = tasks.create("generateAntoraPlaybook",
				GenerateAntoraPlaybook.class);
		configureGenerateAntoraPlaybookTask(project, generateAntoraPlaybookTask);
		Copy copyAntoraPackageJsonTask = tasks.create("copyAntoraPackageJson", Copy.class);
		configureCopyAntoraPackageJsonTask(project, copyAntoraPackageJsonTask);
		NpmInstallTask npmInstallTask = tasks.create("antoraNpmInstall", NpmInstallTask.class);
		configureNpmInstallTask(project, npmInstallTask, copyAntoraPackageJsonTask);
		tasks.withType(GenerateAntoraYmlTask.class, (generateAntoraYmlTask) -> configureGenerateAntoraYmlTask(project,
				generateAntoraYmlTask, dependencyVersionsTask));
		tasks.withType(AntoraTask.class,
				(antoraTask) -> configureAntoraTask(project, antoraTask, npmInstallTask, generateAntoraPlaybookTask));
		project.getExtensions()
			.configure(NodeExtension.class, (nodeExtension) -> configureNodeExtension(project, nodeExtension));
	}

	private void configureGenerateAntoraPlaybookTask(Project project,
			GenerateAntoraPlaybook generateAntoraPlaybookTask) {
		File nodeProjectDir = getNodeProjectDir(project.getBuildDir());
		generateAntoraPlaybookTask.getOutputFile().set(new File(nodeProjectDir, "antora-playbook.yml"));
	}

	private void configureCopyAntoraPackageJsonTask(Project project, Copy copyAntoraPackageJsonTask) {
		copyAntoraPackageJsonTask
			.from(project.getRootProject().file("antora"), (spec) -> spec.include("package.json", "package-lock.json"))
			.into(getNodeProjectDir(project.getBuildDir()));
	}

	private void configureNpmInstallTask(Project project, NpmInstallTask npmInstallTask, Copy copyAntoraPackageJson) {
		npmInstallTask.dependsOn(copyAntoraPackageJson);
		Map<String, String> environment = new HashMap<>();
		environment.put("npm_config_omit", "optional");
		environment.put("npm_config_update_notifier", "false");
		npmInstallTask.getEnvironment().set(environment);
		npmInstallTask.getNpmCommand().set(List.of("ci", "--silent", "--no-progress"));
	}

	private ExtractVersionConstraints addDependencyVersionsTask(Project project) {
		return project.getTasks()
			.create("dependencyVersions", ExtractVersionConstraints.class,
					(task) -> task.enforcedPlatform(DEPENDENCIES_PATH));
	}

	private void configureGenerateAntoraYmlTask(Project project, GenerateAntoraYmlTask generateAntoraYmlTask,
			ExtractVersionConstraints dependencyVersionsTask) {
		generateAntoraYmlTask.getOutputs().doNotCacheIf("getAsciidocAttributes() changes output", (task) -> true);
		generateAntoraYmlTask.dependsOn(dependencyVersionsTask);
		generateAntoraYmlTask.setProperty("componentName", "boot");
		generateAntoraYmlTask.setProperty("outputFile",
				new File(project.getBuildDir(), "generated/docs/antora-yml/antora.yml"));
		generateAntoraYmlTask.setProperty("yml", getDefaultYml(project));
		generateAntoraYmlTask.doFirst((task) -> generateAntoraYmlTask.getAsciidocAttributes()
			.putAll(project.provider(() -> getAsciidocAttributes(project, dependencyVersionsTask))));
	}

	private Map<String, ?> getDefaultYml(Project project) {
		String navFile = null;
		for (String candidate : NAV_FILES) {
			if (project.file(ANTORA_SOURCE_DIR + "/" + candidate).exists()) {
				Assert.state(navFile == null, "Multiple nav files found");
				navFile = candidate;
			}
		}
		Map<String, Object> defaultYml = new LinkedHashMap<>();
		defaultYml.put("title", "Spring Boot");
		if (navFile != null) {
			defaultYml.put("nav", List.of(navFile));
		}
		return defaultYml;
	}

	private Map<String, String> getAsciidocAttributes(Project project,
			ExtractVersionConstraints dependencyVersionsTask) {
		BomExtension bom = (BomExtension) project.project(DEPENDENCIES_PATH).getExtensions().getByName("bom");
		Map<String, String> dependencyVersions = dependencyVersionsTask.getVersionConstraints();
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes(project, bom, dependencyVersions);
		return attributes.get();
	}

	private void configureAntoraTask(Project project, AntoraTask antoraTask, NpmInstallTask npmInstallTask,
			GenerateAntoraPlaybook generateAntoraPlaybookTask) {
		antoraTask.setGroup("Documentation");
		antoraTask.dependsOn(npmInstallTask, generateAntoraPlaybookTask);
		antoraTask.setPlaybook("antora-playbook.yml");
		antoraTask.setUiBundleUrl(getUiBundleUrl(project));
		antoraTask.getArgs().set(project.provider(() -> getAntoraNpxArs(project, antoraTask)));
		project.getPlugins()
			.withType(JavaBasePlugin.class,
					(javaBasePlugin) -> project.getTasks()
						.getByName(JavaBasePlugin.CHECK_TASK_NAME)
						.dependsOn(antoraTask));
	}

	private List<String> getAntoraNpxArs(Project project, AntoraTask antoraTask) {
		logWarningIfNodeModulesInUserHome(project);
		StartParameter startParameter = project.getGradle().getStartParameter();
		boolean showStacktrace = startParameter.getShowStacktrace().name().startsWith("ALWAYS");
		boolean debugLogging = project.getGradle().getStartParameter().getLogLevel() == LogLevel.DEBUG;
		String playbookPath = antoraTask.getPlaybook();
		List<String> arguments = new ArrayList<>();
		arguments.addAll(List.of("--package", "@antora/cli"));
		arguments.add("antora");
		arguments.addAll((!showStacktrace) ? Collections.emptyList() : List.of("--stacktrace"));
		arguments.addAll((!debugLogging) ? List.of("--quiet") : List.of("--log-level", "all"));
		arguments.addAll(List.of("--ui-bundle-url", antoraTask.getUiBundleUrl()));
		arguments.add(playbookPath);
		return arguments;
	}

	private void logWarningIfNodeModulesInUserHome(Project project) {
		if (new File(System.getProperty("user.home"), "node_modules").exists()) {
			project.getLogger()
				.warn("Detected the existence of $HOME/node_modules. This directory is "
						+ "not compatible with this plugin. Please remove it.");
		}
	}

	private String getUiBundleUrl(Project project) {
		try {
			File packageJson = project.getRootProject().file("antora/package.json");
			ObjectMapper objectMapper = new ObjectMapper();
			Map<?, ?> json = objectMapper.readerFor(Map.class).readValue(packageJson);
			Map<?, ?> config = (json != null) ? (Map<?, ?>) json.get("config") : null;
			String url = (config != null) ? (String) config.get("ui-bundle-url") : null;
			Assert.state(StringUtils.hasText(url.toString()), "package.json has not ui-bundle-url config");
			return url;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void configureNodeExtension(Project project, NodeExtension nodeExtension) {
		File buildDir = project.getBuildDir();
		nodeExtension.getWorkDir().set(buildDir.toPath().resolve(".gradle/nodejs").toFile());
		nodeExtension.getNpmWorkDir().set(buildDir.toPath().resolve(".gradle/npm").toFile());
		nodeExtension.getNodeProjectDir().set(getNodeProjectDir(buildDir));
	}

	private File getNodeProjectDir(File buildDir) {
		return buildDir.toPath().resolve(".gradle/nodeproject").toFile();
	}

}
