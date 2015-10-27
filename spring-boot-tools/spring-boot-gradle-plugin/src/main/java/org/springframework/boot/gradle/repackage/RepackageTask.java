/*
 * Copyright 2012-2015 the original author or authors.
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.DefaultLaunchScript;
import org.springframework.boot.loader.tools.LaunchScript;
import org.springframework.boot.loader.tools.Repackager;
import org.springframework.util.FileCopyUtils;

/**
 * Repackage task.
 *
 * @author Phillip Webb
 * @author Janne Valkealahti
 * @author Andy Wilkinson
 */
public class RepackageTask extends DefaultTask {

	private static final long FIND_WARNING_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	private String customConfiguration;

	private Object withJarTask;

	private String mainClass;

	private String classifier;

	private File outputFile;

	public void setCustomConfiguration(String customConfiguration) {
		this.customConfiguration = customConfiguration;
	}

	public Object getWithJarTask() {
		return this.withJarTask;
	}

	public void setWithJarTask(Object withJarTask) {
		this.withJarTask = withJarTask;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getMainClass() {
		return this.mainClass;
	}

	public String getClassifier() {
		return this.classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	void setOutputFile(File file) {
		this.outputFile = file;
	}

	@TaskAction
	public void repackage() {
		Project project = getProject();
		SpringBootPluginExtension extension = project.getExtensions()
				.getByType(SpringBootPluginExtension.class);
		ProjectLibraries libraries = getLibraries();
		project.getTasks().withType(Jar.class, new RepackageAction(extension, libraries));
	}

	public ProjectLibraries getLibraries() {
		Project project = getProject();
		SpringBootPluginExtension extension = project.getExtensions()
				.getByType(SpringBootPluginExtension.class);
		ProjectLibraries libraries = new ProjectLibraries(project, extension);
		if (extension.getProvidedConfiguration() != null) {
			libraries.setProvidedConfigurationName(extension.getProvidedConfiguration());
		}
		if (this.customConfiguration != null) {
			libraries.setCustomConfigurationName(this.customConfiguration);
		}
		else if (extension.getCustomConfiguration() != null) {
			libraries.setCustomConfigurationName(extension.getCustomConfiguration());
		}
		return libraries;
	}

	/**
	 * Action to repackage JARs.
	 */
	private class RepackageAction implements Action<Jar> {

		private final SpringBootPluginExtension extension;

		private final ProjectLibraries libraries;

		RepackageAction(SpringBootPluginExtension extension, ProjectLibraries libraries) {
			this.extension = extension;
			this.libraries = libraries;
		}

		@Override
		public void execute(Jar jarTask) {
			if (!RepackageTask.this.isEnabled()) {
				getLogger().info("Repackage disabled");
				return;
			}
			Object withJarTask = RepackageTask.this.withJarTask;
			if (!isTaskMatch(jarTask, withJarTask)) {
				getLogger().info(
						"Jar task not repackaged (didn't match withJarTask): " + jarTask);
				return;
			}
			File file = jarTask.getArchivePath();
			if (file.exists()) {
				repackage(file);
			}
		}

		private boolean isTaskMatch(Jar task, Object withJarTask) {
			if (withJarTask == null) {
				if ("".equals(task.getClassifier())) {
					Set<Object> tasksWithCustomRepackaging = new HashSet<Object>();
					for (RepackageTask repackageTask : RepackageTask.this.getProject()
							.getTasks().withType(RepackageTask.class)) {
						if (repackageTask.getWithJarTask() != null) {
							tasksWithCustomRepackaging
									.add(repackageTask.getWithJarTask());
						}
					}
					return !tasksWithCustomRepackaging.contains(task);
				}
				return false;
			}
			return task.equals(withJarTask) || task.getName().equals(withJarTask);
		}

		private void repackage(File file) {
			File outputFile = RepackageTask.this.outputFile;
			if (outputFile != null && !file.equals(outputFile)) {
				copy(file, outputFile);
				file = outputFile;
			}
			Repackager repackager = new LoggingRepackager(file);
			setMainClass(repackager);
			if (this.extension.convertLayout() != null) {
				repackager.setLayout(this.extension.convertLayout());
			}
			repackager.setBackupSource(this.extension.isBackupSource());
			try {
				LaunchScript launchScript = getLaunchScript();
				repackager.repackage(file, this.libraries, launchScript);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex.getMessage(), ex);
			}
		}

		private void copy(File source, File dest) {
			try {
				FileCopyUtils.copy(source, dest);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex.getMessage(), ex);
			}
		}

		private void setMainClass(Repackager repackager) {
			String mainClass;
			if (getProject().hasProperty("mainClassName")) {
				mainClass = (String) getProject().property("mainClassName");
			}
			else {
				ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) getProject()
						.getExtensions().getByName("ext");
				mainClass = (String) extraProperties.get("mainClassName");
			}
			if (RepackageTask.this.mainClass != null) {
				mainClass = RepackageTask.this.mainClass;
			}
			else if (this.extension.getMainClass() != null) {
				mainClass = this.extension.getMainClass();
			}
			else {
				Task runTask = getProject().getTasks().findByName("run");
				if (runTask != null && runTask.hasProperty("main")) {
					mainClass = (String) getProject().getTasks().getByName("run")
							.property("main");
				}
			}
			getLogger().info("Setting mainClass: " + mainClass);
			repackager.setMainClass(mainClass);
		}

		private LaunchScript getLaunchScript() throws IOException {
			if (this.extension.isExecutable()
					|| this.extension.getEmbeddedLaunchScript() != null) {
				return new DefaultLaunchScript(this.extension.getEmbeddedLaunchScript(),
						this.extension.getEmbeddedLaunchScriptProperties());
			}
			return null;
		}

	}

	/**
	 * {@link Repackager} that also logs when searching takes too long.
	 */
	private class LoggingRepackager extends Repackager {

		LoggingRepackager(File source) {
			super(source);
		}

		@Override
		protected String findMainMethod(java.util.jar.JarFile source) throws IOException {
			long startTime = System.currentTimeMillis();
			try {
				return super.findMainMethod(source);
			}
			finally {
				long duration = System.currentTimeMillis() - startTime;
				if (duration > FIND_WARNING_TIMEOUT) {
					getLogger().warn("Searching for the main-class is taking "
							+ "some time, consider using setting "
							+ "'springBoot.mainClass'");
				}
			}
		}

	}

}
