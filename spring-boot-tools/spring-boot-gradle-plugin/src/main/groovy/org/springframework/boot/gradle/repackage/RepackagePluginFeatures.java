/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.repackage;

import java.io.File;
import java.io.IOException;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.springframework.boot.gradle.PluginFeatures;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.util.StringUtils;

/**
 * {@link PluginFeatures} to add repackage support.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class RepackagePluginFeatures implements PluginFeatures {

	public static final String REPACKAGE_TASK_NAME = "bootRepackage";

	@Override
	public void apply(Project project) {
		addRepackageTask(project);
		registerRepackageTaskProperty(project);
	}

	private void addRepackageTask(Project project) {
		RepackageTask task = project.getTasks().create(REPACKAGE_TASK_NAME,
				RepackageTask.class);
		task.setDescription("Repackage existing JAR and WAR "
				+ "archives so that they can be executed from the command "
				+ "line using 'java -jar'");
		task.setGroup(BasePlugin.BUILD_GROUP);
		task.dependsOn(project.getConfigurations()
				.getByName(Dependency.ARCHIVES_CONFIGURATION).getAllArtifacts()
				.getBuildDependencies());
		registerOutput(project, task);
		ensureTaskRunsOnAssembly(project, task);
	}

	private void registerOutput(Project project, final RepackageTask task) {
		project.afterEvaluate(new Action<Project>() {
			@Override
			public void execute(Project project) {
				project.getTasks().withType(Jar.class,
						new RegisterInputsOutputsAction(task));
				Object withJar = task.getWithJarTask();
				if (withJar != null) {
					task.dependsOn(withJar);
				}
			}
		});
	}

	private void ensureTaskRunsOnAssembly(Project project, Task task) {
		project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(task);
	}

	/**
	 * Register BootRepackage so that we can use task {@code foo(type: BootRepackage)}.
	 */
	private void registerRepackageTaskProperty(Project project) {
		project.getExtensions().getExtraProperties()
				.set("BootRepackage", RepackageTask.class);
	}

	/**
	 * Register task input/outputs when classifiers are used
	 */
	private static class RegisterInputsOutputsAction implements Action<Jar> {

		private final RepackageTask task;

		private final Project project;

		public RegisterInputsOutputsAction(RepackageTask task) {
			this.task = task;
			this.project = task.getProject();
		}

		@Override
		public void execute(Jar jarTask) {
			if ("".equals(jarTask.getClassifier())) {
				String classifier = this.task.getClassifier();
				if (classifier == null) {
					SpringBootPluginExtension extension = this.project.getExtensions()
							.getByType(SpringBootPluginExtension.class);
					classifier = extension.getClassifier();
					this.task.setClassifier(classifier);
				}
				if (classifier != null) {
					setupInputOutputs(jarTask, classifier);
				}
			}
		}

		private void setupInputOutputs(Jar jarTask, String classifier) {
			Logger logger = this.project.getLogger();
			logger.debug("Using classifier: " + classifier + " for task "
					+ task.getName());
			File inputFile = jarTask.getArchivePath();
			String outputName = inputFile.getName();
			outputName = StringUtils.stripFilenameExtension(outputName) + "-"
					+ classifier + "." + StringUtils.getFilenameExtension(outputName);
			File outputFile = new File(inputFile.getParentFile(), outputName);
			this.task.getInputs().file(jarTask);
			addLibraryDependencies(this.task);
			this.task.getOutputs().file(outputFile);
			this.task.setOutputFile(outputFile);
		}

		private void addLibraryDependencies(final RepackageTask task) {
			try {
				task.getLibraries().doWithLibraries(new LibraryCallback() {
					@Override
					public void library(Library library) throws IOException {
						task.getInputs().file(library.getFile());
					}
				});
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
