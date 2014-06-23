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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.springframework.boot.gradle.PluginFeatures;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.util.StringUtils;

/**
 * {@link PluginFeatures} to add repackage support.
 *
 * @author Phillip Webb
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
		task.dependsOn(project.getConfigurations().getByName(
				Dependency.ARCHIVES_CONFIGURATION).getAllArtifacts().getBuildDependencies());
		registerOutput(project, task);
		ensureTaskRunsOnAssembly(project, task);
	}

	private void registerOutput(Project project, final RepackageTask task) {
		project.afterEvaluate(new Action<Project>() {

			@Override
			public void execute(Project project) {
				project.getTasks().withType(Jar.class, new OutputAction(task));
				Object withJar = task.getWithJarTask();
				if (withJar!=null) {
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
		project.getExtensions().getExtraProperties().set("BootRepackage",
				RepackageTask.class);
	}

	private class OutputAction implements Action<Jar> {

		private RepackageTask task;

		public OutputAction(RepackageTask task) {
			this.task = task;
		}

		@Override
		public void execute(Jar archive) {
			if ("".equals(archive.getClassifier())) {
				setClassifier(archive);
				File file = archive.getArchivePath();
				String classifier = this.task.getClassifier();
				if (classifier != null) {
					this.task.getInputs().file(archive);
					task.getInputs().file(task.getDependencies());
					String withClassifer = file.getName();
					withClassifer = StringUtils.stripFilenameExtension(withClassifer)
							+ "-" + classifier + "."
							+ StringUtils.getFilenameExtension(withClassifer);
					File out = new File(file.getParentFile(), withClassifer);
					file = out;
					this.task.getOutputs().file(file);
					this.task.setOutputFile(file);
				}
			}

		}

		private void setClassifier(Jar archive) {
			Project project = task.getProject();
			String classifier = null;
			SpringBootPluginExtension extension = project.getExtensions().getByType(
					SpringBootPluginExtension.class);
			if (task.getClassifier() != null) {
				classifier = task.getClassifier();
			} else if (extension.getClassifier() != null) {
				classifier = extension.getClassifier();
			}
			if (classifier != null) {
				project.getLogger().info("Setting classifier: " + classifier);
				task.setClassifier(classifier);
			}
		}

	}

}
