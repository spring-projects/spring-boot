/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.gradle.repackage;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.Repackager;
import org.springframework.boot.loader.tools.Repackager.MainClassTimeoutWarningListener;
import org.springframework.util.FileCopyUtils;

/**
 * Repackage task.
 *
 * @author Phillip Webb
 * @author Janne Valkealahti
 * @author Andy Wilkinson
 */
public class RepackageTask extends DefaultTask {

	private String customConfiguration;

	private Object withJarTask;

	private String mainClass;

	private String classifier;

	private File outputFile;

	private Boolean excludeDevtools;

	private Boolean executable;

	private File embeddedLaunchScript;

	private Map<String, String> embeddedLaunchScriptProperties;

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

	public Boolean getExcludeDevtools() {
		return this.excludeDevtools;
	}

	public void setExcludeDevtools(Boolean excludeDevtools) {
		this.excludeDevtools = excludeDevtools;
	}

	public Boolean getExecutable() {
		return this.executable;
	}

	public void setExecutable(Boolean executable) {
		this.executable = executable;
	}

	public File getEmbeddedLaunchScript() {
		return this.embeddedLaunchScript;
	}

	public void setEmbeddedLaunchScript(File embeddedLaunchScript) {
		this.embeddedLaunchScript = embeddedLaunchScript;
	}

	public Map<String, String> getEmbeddedLaunchScriptProperties() {
		return this.embeddedLaunchScriptProperties;
	}

	public void setEmbeddedLaunchScriptProperties(
			Map<String, String> embeddedLaunchScriptProperties) {
		this.embeddedLaunchScriptProperties = embeddedLaunchScriptProperties;
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
		ProjectLibraries libraries = new ProjectLibraries(project, extension,
				(this.excludeDevtools != null) ? this.excludeDevtools
						: extension.isExcludeDevtools());
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

		@SuppressWarnings("deprecation")
		private void repackage(File file) {
			File outputFile = RepackageTask.this.outputFile;
			if (outputFile != null && !file.equals(outputFile)) {
				copy(file, outputFile);
				file = outputFile;
			}
			Repackager repackager = new Repackager(file,
					this.extension.getLayoutFactory());
			repackager.addMainClassTimeoutWarningListener(
					new LoggingMainClassTimeoutWarningListener());
			setMainClass(repackager);
			Layout layout = this.extension.convertLayout();
			if (layout != null) {
				if (layout instanceof Layouts.Module) {
					getLogger().warn("Module layout is deprecated. Please use a custom"
							+ " LayoutFactory instead.");
				}
				repackager.setLayout(layout);
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
			String mainClassName = getMainClassNameProperty();
			if (RepackageTask.this.mainClass != null) {
				mainClassName = RepackageTask.this.mainClass;
			}
			else if (this.extension.getMainClass() != null) {
				mainClassName = this.extension.getMainClass();
			}
			else {
				Task runTask = getProject().getTasks().findByName("run");
				if (runTask != null && runTask.hasProperty("main")) {
					mainClassName = (String) getProject().getTasks().getByName("run")
							.property("main");
				}
			}
			if (mainClassName != null) {
				getLogger().info("Setting mainClass: " + mainClassName);
				repackager.setMainClass(mainClassName);
			}
			else {
				getLogger().info("No mainClass configured");
			}
		}

		private String getMainClassNameProperty() {
			if (getProject().hasProperty("mainClassName")) {
				return (String) getProject().property("mainClassName");
			}
			ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) getProject()
					.getExtensions().getByName("ext");
			if (extraProperties.has("mainClassName")) {
				return (String) extraProperties.get("mainClassName");
			}
			return null;
		}

		private LaunchScript getLaunchScript() throws IOException {
			if (isExecutable() || getEmbeddedLaunchScript() != null) {
				return new DefaultLaunchScript(getEmbeddedLaunchScript(),
						getEmbeddedLaunchScriptProperties());
			}
			return null;
		}

		private boolean isExecutable() {
			return (RepackageTask.this.executable != null) ? RepackageTask.this.executable
					: this.extension.isExecutable();
		}

		private File getEmbeddedLaunchScript() {
			return (RepackageTask.this.embeddedLaunchScript != null)
					? RepackageTask.this.embeddedLaunchScript
					: this.extension.getEmbeddedLaunchScript();
		}

		private Map<String, String> getEmbeddedLaunchScriptProperties() {
			return (RepackageTask.this.embeddedLaunchScriptProperties != null)
					? RepackageTask.this.embeddedLaunchScriptProperties
					: this.extension.getEmbeddedLaunchScriptProperties();
		}

	}

	/**
	 * {@link Repackager} that also logs when searching takes too long.
	 */
	private class LoggingMainClassTimeoutWarningListener
			implements MainClassTimeoutWarningListener {

		@Override
		public void handleTimeoutWarning(long duration, String mainMethod) {
			getLogger().warn("Searching for the main-class is taking "
					+ "some time, consider using setting " + "'springBoot.mainClass'");
		}

	}

}
