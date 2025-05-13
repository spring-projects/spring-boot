/*
 * Copyright 2012-2025 the original author or authors.
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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.build.antora.AntoraAsciidocAttributes;
import org.springframework.boot.build.antora.GenerateAntoraPlaybook;
import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Conventions that are applied in the presence of the {@link AntoraPlugin}.
 *
 * @author Phillip Webb
 */
public class AntoraConventions {

	private static final String DEPENDENCIES_PATH = ":spring-boot-project:spring-boot-dependencies";

	private static final List<String> NAV_FILES = List.of("nav.adoc", "local-nav.adoc");

	/**
	 * Default Antora source directory.
	 */
	public static final String ANTORA_SOURCE_DIR = "src/docs/antora";

	/**
	 * Name of the {@link GenerateAntoraPlaybook} task.
	 */
	public static final String GENERATE_ANTORA_PLAYBOOK_TASK_NAME = "generateAntoraPlaybook";

	void apply(Project project) {
		project.getPlugins().withType(AntoraPlugin.class, (antoraPlugin) -> apply(project, antoraPlugin));
	}

	private void apply(Project project, AntoraPlugin antoraPlugin) {
		Configuration resolvedBom = project.getConfigurations().create("resolveBom");
		project.getDependencies()
			.add(resolvedBom.getName(), project.getDependencies()
				.project(Map.of("path", DEPENDENCIES_PATH, "configuration", "resolvedBom")));
		project.getPlugins().apply(GenerateAntoraYmlPlugin.class);
		TaskContainer tasks = project.getTasks();
		TaskProvider<GenerateAntoraPlaybook> generateAntoraPlaybookTask = tasks.register(
				GENERATE_ANTORA_PLAYBOOK_TASK_NAME, GenerateAntoraPlaybook.class,
				(task) -> configureGenerateAntoraPlaybookTask(project, task));
		TaskProvider<Copy> copyAntoraPackageJsonTask = tasks.register("copyAntoraPackageJson", Copy.class,
				(task) -> configureCopyAntoraPackageJsonTask(project, task));
		TaskProvider<NpmInstallTask> npmInstallTask = tasks.register("antoraNpmInstall", NpmInstallTask.class,
				(task) -> configureNpmInstallTask(project, task, copyAntoraPackageJsonTask,
						generateAntoraPlaybookTask));
		tasks.withType(GenerateAntoraYmlTask.class,
				(generateAntoraYmlTask) -> configureGenerateAntoraYmlTask(project, generateAntoraYmlTask, resolvedBom));
		tasks.withType(AntoraTask.class,
				(antoraTask) -> configureAntoraTask(project, antoraTask, npmInstallTask, generateAntoraPlaybookTask));
		project.getExtensions()
			.configure(NodeExtension.class, (nodeExtension) -> configureNodeExtension(project, nodeExtension));
	}

	private void configureGenerateAntoraPlaybookTask(Project project,
			GenerateAntoraPlaybook generateAntoraPlaybookTask) {
		Provider<Directory> nodeProjectDir = getNodeProjectDir(project);
		generateAntoraPlaybookTask.getOutputFile()
			.set(nodeProjectDir.map((directory) -> directory.file("antora-playbook.yml")));
	}

	private void configureCopyAntoraPackageJsonTask(Project project, Copy copyAntoraPackageJsonTask) {
		copyAntoraPackageJsonTask
			.from(project.getRootProject().file("antora"),
					(spec) -> spec.include("package.json", "package-lock.json", "patches/**"))
			.into(getNodeProjectDir(project));
	}

	private void configureNpmInstallTask(Project project, NpmInstallTask npmInstallTask,
			TaskProvider<Copy> copyAntoraPackageJson, TaskProvider<GenerateAntoraPlaybook> generateAntoraPlaybookTask) {
		npmInstallTask.dependsOn(copyAntoraPackageJson);
		npmInstallTask.dependsOn(generateAntoraPlaybookTask);
		Map<String, String> environment = new HashMap<>();
		environment.put("npm_config_omit", "optional");
		environment.put("npm_config_update_notifier", "false");
		npmInstallTask.getEnvironment().set(environment);
		npmInstallTask.getNpmCommand().set(List.of("ci", "--silent", "--no-progress"));

		npmInstallTask.getInputs()
			.files(project.getLayout().getBuildDirectory().dir(".gradle/nodejs"))
			.withPropertyName("antoraNodeJs")
			.withPathSensitivity(PathSensitivity.RELATIVE);

		npmInstallTask.getInputs()
			.files(getNodeProjectDir(project))
			.withPropertyName("antoraNodeProjectDir")
			.withPathSensitivity(PathSensitivity.RELATIVE);
	}

	private void configureGenerateAntoraYmlTask(Project project, GenerateAntoraYmlTask generateAntoraYmlTask,
			Configuration resolvedBom) {
		generateAntoraYmlTask.getOutputs().doNotCacheIf("getAsciidocAttributes() changes output", (task) -> true);
		generateAntoraYmlTask.dependsOn(resolvedBom);
		generateAntoraYmlTask.setProperty("componentName", "boot");
		generateAntoraYmlTask.setProperty("outputFile",
				project.getLayout().getBuildDirectory().file("generated/docs/antora-yml/antora.yml"));
		generateAntoraYmlTask.setProperty("yml", getDefaultYml(project));
		generateAntoraYmlTask.getAsciidocAttributes().putAll(getAsciidocAttributes(project, resolvedBom));
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

	private Provider<Map<String, String>> getAsciidocAttributes(Project project, FileCollection resolvedBoms) {
		return project.provider(() -> {
			BomExtension bom = (BomExtension) project.project(DEPENDENCIES_PATH).getExtensions().getByName("bom");
			ResolvedBom resolvedBom = ResolvedBom.readFrom(resolvedBoms.getSingleFile());
			return new AntoraAsciidocAttributes(project, bom, resolvedBom).get();
		});
	}

	private void configureAntoraTask(Project project, AntoraTask antoraTask,
			TaskProvider<NpmInstallTask> npmInstallTask,
			TaskProvider<GenerateAntoraPlaybook> generateAntoraPlaybookTask) {
		antoraTask.setGroup("Documentation");
		antoraTask.dependsOn(npmInstallTask, generateAntoraPlaybookTask);

		antoraTask.getInputs()
			.file(generateAntoraPlaybookTask.flatMap(GenerateAntoraPlaybook::getOutputFile))
			.withPropertyName("antoraPlaybookFile")
			.withPathSensitivity(PathSensitivity.RELATIVE);

		antoraTask.getInputs()
			.files(project.getLayout().getBuildDirectory().dir(".gradle/nodejs"))
			.withPropertyName("antoraNodeJs")
			.withPathSensitivity(PathSensitivity.RELATIVE);

		antoraTask.getInputs()
			.files(getNodeProjectDir(project))
			.withPropertyName("antoraNodeProjectDir")
			.withPathSensitivity(PathSensitivity.RELATIVE);

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
		nodeExtension.getWorkDir().set(project.getLayout().getBuildDirectory().dir(".gradle/nodejs"));
		nodeExtension.getNpmWorkDir().set(project.getLayout().getBuildDirectory().dir(".gradle/npm"));
		nodeExtension.getNodeProjectDir().set(getNodeProjectDir(project));
	}

	private Provider<Directory> getNodeProjectDir(Project project) {
		return project.getLayout().getBuildDirectory().dir(".gradle/nodeproject");
	}

}
