/*
 * Copyright 2019-2020 the original author or authors.
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
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.gradle.jvm.AbstractAsciidoctorTask;
import org.asciidoctor.gradle.jvm.AsciidoctorJExtension;
import org.asciidoctor.gradle.jvm.AsciidoctorJPlugin;
import org.asciidoctor.gradle.jvm.AsciidoctorTask;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.artifactory.ArtifactoryRepository;
import org.springframework.util.StringUtils;

/**
 * Conventions that are applied in the presence of the {@link AsciidoctorJPlugin}. When
 * the plugin is applied:
 *
 * <ul>
 * <li>All warnings are made fatal.
 * <li>The version of AsciidoctorJ is upgraded to 2.4.1.
 * <li>A task is created to resolve and unzip our documentation resources (CSS and
 * Javascript).
 * <li>For each {@link AsciidoctorTask} (HTML only):
 * <ul>
 * <li>A task is created to sync the documentation resources to its output directory.
 * <li>{@code doctype} {@link AsciidoctorTask#options(Map) option} is configured.
 * <li>{@link AsciidoctorTask#attributes(Map) Attributes} are configured for syntax
 * highlighting, CSS styling, docinfo, etc.
 * </ul>
 * <li>For each {@link AbstractAsciidoctorTask} (HTML and PDF):
 * <ul>
 * <li>{@link AsciidoctorTask#attributes(Map) Attributes} are configured to enable
 * warnings for references to missing attributes, the GitHub tag, the Artifactory repo for
 * the current version, etc.
 * <li>{@link AbstractAsciidoctorTask#baseDirFollowsSourceDir() baseDirFollowsSourceDir()}
 * is enabled.
 * </ul>
 * </ul>
 *
 * @author Andy Wilkinson
 */
class AsciidoctorConventions {

	private static final String ASCIIDOCTORJ_VERSION = "2.4.1";

	void apply(Project project) {
		project.getPlugins().withType(AsciidoctorJPlugin.class, (asciidoctorPlugin) -> {
			configureDocResourcesRepository(project);
			makeAllWarningsFatal(project);
			upgradeAsciidoctorJVersion(project);
			UnzipDocumentationResources unzipResources = createUnzipDocumentationResourcesTask(project);
			project.getTasks().withType(AbstractAsciidoctorTask.class, (asciidoctorTask) -> {
				configureCommonAttributes(project, asciidoctorTask);
				configureOptions(asciidoctorTask);
				asciidoctorTask.baseDirFollowsSourceDir();
				Sync syncSource = createSyncDocumentationSourceTask(project, asciidoctorTask);
				if (asciidoctorTask instanceof AsciidoctorTask) {
					configureHtmlOnlyAttributes(asciidoctorTask);
					syncSource.from(unzipResources, (resources) -> resources.into("asciidoc"));
					asciidoctorTask.doFirst(new Action<Task>() {

						@Override
						public void execute(Task task) {
							project.copy((spec) -> {
								spec.from(asciidoctorTask.getSourceDir());
								spec.into(asciidoctorTask.getOutputDir());
								spec.include("css/**", "js/**");
							});
						}

					});
				}
			});
		});
	}

	private void configureDocResourcesRepository(Project project) {
		project.getRepositories().maven((mavenRepo) -> {
			mavenRepo.setUrl(URI.create("https://repo.spring.io/release"));
			mavenRepo.mavenContent((mavenContent) -> mavenContent.includeGroup("io.spring.docresources"));
		});
	}

	private void makeAllWarningsFatal(Project project) {
		project.getExtensions().getByType(AsciidoctorJExtension.class).fatalWarnings(".*");
	}

	private void upgradeAsciidoctorJVersion(Project project) {
		project.getExtensions().getByType(AsciidoctorJExtension.class).setVersion(ASCIIDOCTORJ_VERSION);
	}

	private UnzipDocumentationResources createUnzipDocumentationResourcesTask(Project project) {
		Configuration documentationResources = project.getConfigurations().maybeCreate("documentationResources");
		documentationResources.getDependencies()
				.add(project.getDependencies().create("io.spring.docresources:spring-doc-resources:0.2.5"));
		UnzipDocumentationResources unzipResources = project.getTasks().create("unzipDocumentationResources",
				UnzipDocumentationResources.class);
		unzipResources.setResources(documentationResources);
		unzipResources.setOutputDir(new File(project.getBuildDir(), "docs/resources"));
		return unzipResources;
	}

	private Sync createSyncDocumentationSourceTask(Project project, AbstractAsciidoctorTask asciidoctorTask) {
		Sync syncDocumentationSource = project.getTasks()
				.create("syncDocumentationSourceFor" + StringUtils.capitalize(asciidoctorTask.getName()), Sync.class);
		File syncedSource = new File(project.getBuildDir(), "docs/src/" + asciidoctorTask.getName());
		syncDocumentationSource.setDestinationDir(syncedSource);
		syncDocumentationSource.from("src/docs/");
		asciidoctorTask.dependsOn(syncDocumentationSource);
		asciidoctorTask.getInputs().dir(syncedSource);
		asciidoctorTask.setSourceDir(project.relativePath(new File(syncedSource, "asciidoc/")));
		return syncDocumentationSource;
	}

	private void configureOptions(AbstractAsciidoctorTask asciidoctorTask) {
		asciidoctorTask.options(Collections.singletonMap("doctype", "book"));
	}

	private void configureHtmlOnlyAttributes(AbstractAsciidoctorTask asciidoctorTask) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("source-highlighter", "highlightjs");
		attributes.put("highlightjsdir", "js/highlight");
		attributes.put("highlightjs-theme", "github");
		attributes.put("linkcss", true);
		attributes.put("icons", "font");
		attributes.put("stylesheet", "css/spring.css");
		asciidoctorTask.attributes(attributes);
	}

	private void configureCommonAttributes(Project project, AbstractAsciidoctorTask asciidoctorTask) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("attribute-missing", "warn");
		attributes.put("github-tag", determineGitHubTag(project));
		attributes.put("spring-boot-artifactory-repo", ArtifactoryRepository.forProject(project));
		attributes.put("revnumber", null);
		asciidoctorTask.attributes(attributes);
	}

	private String determineGitHubTag(Project project) {
		String version = "v" + project.getVersion();
		return (version.endsWith("-SNAPSHOT")) ? "2.4.x" : version;
	}

	/**
	 * {@link Task} for unzipping the documentation resources.
	 */
	public static class UnzipDocumentationResources extends DefaultTask {

		private FileCollection resources;

		private File outputDir;

		@InputFiles
		public FileCollection getResources() {
			return this.resources;
		}

		public void setResources(FileCollection resources) {
			this.resources = resources;
		}

		@OutputDirectory
		public File getOutputDir() {
			return this.outputDir;
		}

		public void setOutputDir(File outputDir) {
			this.outputDir = outputDir;
		}

		@TaskAction
		void syncDocumentationResources() {
			getProject().sync((copySpec) -> {
				copySpec.into(this.outputDir);
				for (File resource : this.resources) {
					copySpec.from(getProject().zipTree(resource));
				}
			});
		}

	}

}
