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
import java.util.concurrent.TimeUnit;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.Repackager;
import org.springframework.util.FileCopyUtils;

/**
 * Repackage task.
 *
 * @author Phillip Webb
 * @author Janne Valkealahti
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
		return withJarTask;
	}

	public void setWithJarTask(Object withJarTask) {
		this.withJarTask = withJarTask;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getMainClass() {
		return mainClass;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	@TaskAction
	public void repackage() {
		Project project = getProject();
		SpringBootPluginExtension extension = project.getExtensions().getByType(
				SpringBootPluginExtension.class);
		ProjectLibraries libraries = getLibraries();
		project.getTasks().withType(Jar.class, new RepackageAction(extension, libraries));
	}

	public ProjectLibraries getLibraries() {
		Project project = getProject();
		SpringBootPluginExtension extension = project.getExtensions().getByType(
				SpringBootPluginExtension.class);
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

		public RepackageAction(SpringBootPluginExtension extension,
				ProjectLibraries libraries) {
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
			if (isTaskMatch(jarTask, withJarTask)) {
				getLogger().info(
						"Jar task not repackaged (didn't match withJarTask): " + jarTask);
				return;
			}
			if ("".equals(jarTask.getClassifier())
					|| RepackageTask.this.withJarTask != null) {
				File file = jarTask.getArchivePath();
				if (file.exists()) {
					repackage(file);
				}
			}
		}

		private boolean isTaskMatch(Jar task, Object compare) {
			if (compare == null) {
				return false;
			}
			TaskContainer tasks = getProject().getTasks();
			return task.equals(compare) || task.equals(tasks.findByName(task.toString()));
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
				repackager.repackage(file, this.libraries);
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
			String mainClass = (String) getProject().property("mainClassName");
			if (RepackageTask.this.mainClass != null) {
				mainClass = RepackageTask.this.mainClass;
			}
			else if (this.extension.getMainClass() != null) {
				mainClass = this.extension.getMainClass();
			}
			else if (getProject().getTasks().getByName("run").hasProperty("main")) {
				mainClass = (String) getProject().getTasks().getByName("run")
						.property("main");
			}
			getLogger().info("Setting mainClass: " + mainClass);
			repackager.setMainClass(mainClass);
		}
	}

	/**
	 * {@link Repackager} that also logs when searching takes too long.
	 */
	private class LoggingRepackager extends Repackager {

		public LoggingRepackager(File source) {
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
					getLogger().warn(
							"Searching for the main-class is taking "
									+ "some time, consider using setting "
									+ "'springBoot.mainClass'");
				}
			}
		};
	}

	void setOutputFile(File file) {
		this.outputFile = file;
	}

}
