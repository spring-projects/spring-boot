/*
 * Copyright 2012-2013 the original author or authors.
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

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.Repackager;

/**
 * Repackage task.
 * 
 * @author Phillip Webb
 */
public class Repackage extends DefaultTask {

	@TaskAction
	public void repackage() {
		Project project = getProject();
		final SpringBootPluginExtension extension = project.getExtensions().getByType(
				SpringBootPluginExtension.class);
		final ProjectLibraries libraries = new ProjectLibraries(project);
		if (extension.getProvidedConfiguration() != null) {
			libraries.setProvidedConfigurationName(extension.getProvidedConfiguration());
		}
		project.getTasks().withType(Jar.class, new Action<Jar>() {

			@Override
			public void execute(Jar archive) {
				if ("".equals(archive.getClassifier())) {
					File file = archive.getArchivePath();
					if (file.exists()) {
						Repackager repackager = new Repackager(file);
						repackager.setMainClass(extension.getMainClass());
						if (extension.convertLayout() != null) {
							repackager.setLayout(extension.convertLayout());
						}
						repackager.setBackupSource(extension.isBackupSource());
						try {
							repackager.repackage(libraries);
						} catch (IOException ex) {
							throw new IllegalStateException(ex.getMessage(), ex);
						}
					}
				}
			}
		});
	}
}
