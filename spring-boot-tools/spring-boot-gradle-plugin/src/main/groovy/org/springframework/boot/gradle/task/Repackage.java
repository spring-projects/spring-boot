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

package org.springframework.boot.gradle.task;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.Repackager;

/**
 * Repackage task.
 * 
 * @author Phillip Webb
 * @author Janne Valkealahti
 */
public class Repackage extends DefaultTask {

	private static final long FIND_WARNING_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	private String customConfiguration;

	private Object withJarTask;

	private String mainClass;

	public void setCustomConfiguration(String customConfiguration) {
		this.customConfiguration = customConfiguration;
	}

	public void setWithJarTask(Object withJarTask) {
		this.withJarTask = withJarTask;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	@TaskAction
	public void repackage() {
		Project project = getProject();
		SpringBootPluginExtension extension = project.getExtensions().getByType(
				SpringBootPluginExtension.class);
		ProjectLibraries libraries = new ProjectLibraries(project);
		if (extension.getProvidedConfiguration() != null) {
			libraries.setProvidedConfigurationName(extension.getProvidedConfiguration());
		}
		if (this.customConfiguration != null) {
			libraries.setCustomConfigurationName(this.customConfiguration);
		}
		else if (extension.getCustomConfiguration() != null) {
			libraries.setCustomConfigurationName(extension.getCustomConfiguration());
		}
		JavaExec runner = (JavaExec) project.getTasks().findByName("run");
		if (runner!=null && mainClass==null) {
			getLogger().info("Found main in run task: " + runner.getMain());
			setMainClass(runner.getMain());
		}
		project.getTasks().withType(Jar.class, new RepackageAction(extension, libraries));
	}

	private class RepackageAction implements Action<Jar> {

		private final SpringBootPluginExtension extension;

		private final ProjectLibraries libraries;

		public RepackageAction(SpringBootPluginExtension extension,
				ProjectLibraries libraries) {
			this.extension = extension;
			this.libraries = libraries;
		}

		@Override
		public void execute(Jar archive) {
			// if withJarTask is set, compare tasks and bail out if we didn't match
			if (Repackage.this.withJarTask != null
					&& !archive.equals(Repackage.this.withJarTask)) {
				return;
			}

			if ("".equals(archive.getClassifier())) {
				File file = archive.getArchivePath();
				if (file.exists()) {
					Repackager repackager = new LoggingRepackager(file);
					repackager.setMainClass(this.extension.getMainClass());
					if (Repackage.this.mainClass != null) {
						repackager.setMainClass(Repackage.this.mainClass);
					}
					if (this.extension.convertLayout() != null) {
						repackager.setLayout(this.extension.convertLayout());
					}
					repackager.setBackupSource(this.extension.isBackupSource());
					try {
						repackager.repackage(this.libraries);
					}
					catch (IOException ex) {
						throw new IllegalStateException(ex.getMessage(), ex);
					}
				}
			}
		}
	}

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
}
