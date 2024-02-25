/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.gradle.jvm.AbstractAsciidoctorTask;
import org.asciidoctor.gradle.jvm.AsciidoctorJExtension;
import org.asciidoctor.gradle.jvm.AsciidoctorJPlugin;
import org.asciidoctor.gradle.jvm.AsciidoctorTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.Sync;

import org.springframework.boot.build.artifacts.ArtifactRelease;
import org.springframework.util.StringUtils;

/**
 * Conventions that are applied in the presence of the {@link AsciidoctorJPlugin}. When
 * the plugin is applied:
 *
 * <ul>
 * <li>All warnings are made fatal.
 * <li>The version of AsciidoctorJ is upgraded to 2.4.3.
 * <li>An {@code asciidoctorExtensions} configuration is created.
 * <li>For each {@link AsciidoctorTask} (HTML only):
 * <ul>
 * <li>A task is created to sync the documentation resources to its output directory.
 * <li>{@code doctype} {@link AsciidoctorTask#options(Map) option} is configured.
 * <li>The {@code backend} is configured.
 * </ul>
 * <li>For each {@link AbstractAsciidoctorTask} (HTML and PDF):
 * <ul>
 * <li>{@link AsciidoctorTask#attributes(Map) Attributes} are configured to enable
 * warnings for references to missing attributes, the GitHub tag, the Artifactory repo for
 * the current version, etc.
 * <li>{@link AbstractAsciidoctorTask#baseDirFollowsSourceDir() baseDirFollowsSourceDir()}
 * is enabled.
 * <li>{@code asciidoctorExtensions} is added to the task's configurations.
 * </ul>
 * </ul>
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class AsciidoctorConventions {

	private static final String ASCIIDOCTORJ_VERSION = "2.4.3";

	private static final String EXTENSIONS_CONFIGURATION_NAME = "asciidoctorExtensions";

	/**
	 * Applies the necessary conventions for Asciidoctor to the given project. This
	 * includes making all warnings fatal, upgrading the AsciidoctorJ version, creating
	 * the Asciidoctor extensions configuration, and configuring the Asciidoctor tasks.
	 * @param project the project to apply the conventions to
	 */
	void apply(Project project) {
		project.getPlugins().withType(AsciidoctorJPlugin.class, (asciidoctorPlugin) -> {
			makeAllWarningsFatal(project);
			upgradeAsciidoctorJVersion(project);
			createAsciidoctorExtensionsConfiguration(project);
			project.getTasks()
				.withType(AbstractAsciidoctorTask.class,
						(asciidoctorTask) -> configureAsciidoctorTask(project, asciidoctorTask));
		});
	}

	/**
	 * Makes all warnings fatal for the given project.
	 * @param project the project for which to make warnings fatal
	 */
	private void makeAllWarningsFatal(Project project) {
		project.getExtensions().getByType(AsciidoctorJExtension.class).fatalWarnings(".*");
	}

	/**
	 * Upgrades the version of AsciidoctorJ in the given project.
	 * @param project the project to upgrade AsciidoctorJ version in
	 */
	private void upgradeAsciidoctorJVersion(Project project) {
		project.getExtensions().getByType(AsciidoctorJExtension.class).setVersion(ASCIIDOCTORJ_VERSION);
	}

	/**
	 * Creates the Asciidoctor extensions configuration for the given project.
	 * @param project the project for which the configuration is created
	 */
	private void createAsciidoctorExtensionsConfiguration(Project project) {
		project.getConfigurations().create(EXTENSIONS_CONFIGURATION_NAME, (configuration) -> {
			project.getConfigurations()
				.matching((candidate) -> "dependencyManagement".equals(candidate.getName()))
				.all(configuration::extendsFrom);
			configuration.getDependencies()
				.add(project.getDependencies()
					.create("io.spring.asciidoctor.backends:spring-asciidoctor-backends:0.0.5"));
			configuration.getDependencies()
				.add(project.getDependencies().create("org.asciidoctor:asciidoctorj-pdf:1.5.3"));
		});
	}

	/**
	 * Configures the given Asciidoctor task with the necessary attributes, options, and
	 * fork options.
	 * @param project the project to configure the task for
	 * @param asciidoctorTask the Asciidoctor task to configure
	 */
	private void configureAsciidoctorTask(Project project, AbstractAsciidoctorTask asciidoctorTask) {
		asciidoctorTask.configurations(EXTENSIONS_CONFIGURATION_NAME);
		configureCommonAttributes(project, asciidoctorTask);
		configureOptions(asciidoctorTask);
		configureForkOptions(asciidoctorTask);
		asciidoctorTask.baseDirFollowsSourceDir();
		createSyncDocumentationSourceTask(project, asciidoctorTask);
		if (asciidoctorTask instanceof AsciidoctorTask task) {
			boolean pdf = task.getName().toLowerCase().contains("pdf");
			String backend = (!pdf) ? "spring-html" : "spring-pdf";
			task.outputOptions((outputOptions) -> outputOptions.backends(backend));
		}
	}

	/**
	 * Configures common attributes for the given project and Asciidoctor task.
	 * @param project The project to configure attributes for.
	 * @param asciidoctorTask The Asciidoctor task to configure attributes for.
	 */
	private void configureCommonAttributes(Project project, AbstractAsciidoctorTask asciidoctorTask) {
		ArtifactRelease artifacts = ArtifactRelease.forProject(project);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("attribute-missing", "warn");
		attributes.put("github-tag", determineGitHubTag(project));
		attributes.put("artifact-release-type", artifacts.getType());
		attributes.put("artifact-download-repo", artifacts.getDownloadRepo());
		attributes.put("revnumber", null);
		asciidoctorTask.attributes(attributes);
	}

	// See https://github.com/asciidoctor/asciidoctor-gradle-plugin/issues/597
	private void configureForkOptions(AbstractAsciidoctorTask asciidoctorTask) {
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
			asciidoctorTask.forkOptions((options) -> options.jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
					"--add-opens", "java.base/java.io=ALL-UNNAMED"));
		}
	}

	/**
	 * Determines the GitHub tag for the given project.
	 * @param project the project for which to determine the GitHub tag
	 * @return the GitHub tag for the project
	 */
	private String determineGitHubTag(Project project) {
		String version = "v" + project.getVersion();
		return (version.endsWith("-SNAPSHOT")) ? "main" : version;
	}

	/**
	 * Configures the options for the given {@link AbstractAsciidoctorTask}.
	 * @param asciidoctorTask the {@link AbstractAsciidoctorTask} to configure options for
	 */
	private void configureOptions(AbstractAsciidoctorTask asciidoctorTask) {
		asciidoctorTask.options(Collections.singletonMap("doctype", "book"));
	}

	/**
	 * Creates a Sync task for synchronizing the documentation source with the Asciidoctor
	 * task.
	 * @param project The project object.
	 * @param asciidoctorTask The Asciidoctor task.
	 * @return The created Sync task.
	 */
	private Sync createSyncDocumentationSourceTask(Project project, AbstractAsciidoctorTask asciidoctorTask) {
		Sync syncDocumentationSource = project.getTasks()
			.create("syncDocumentationSourceFor" + StringUtils.capitalize(asciidoctorTask.getName()), Sync.class);
		File syncedSource = new File(project.getBuildDir(), "docs/src/" + asciidoctorTask.getName());
		syncDocumentationSource.setDestinationDir(syncedSource);
		syncDocumentationSource.from("src/docs/");
		asciidoctorTask.dependsOn(syncDocumentationSource);
		asciidoctorTask.getInputs()
			.dir(syncedSource)
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName("synced source");
		asciidoctorTask.setSourceDir(project.relativePath(new File(syncedSource, "asciidoc/")));
		return syncDocumentationSource;
	}

}
