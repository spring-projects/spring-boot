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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.springframework.boot.loader.tools.FileUtils;
import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Run the project from Gradle.
 * 
 * @author Dave Syer
 */
public class RunApp extends DefaultTask {

	@TaskAction
	public void runApp() {

		final Project project = getProject();
		final SourceSet main = ComputeMain.findMainSourceSet(project);
		final File outputDir = (main == null ? null : main.getOutput().getResourcesDir());
		final Set<File> allResources = new LinkedHashSet<File>();
		if (main != null) {
			allResources.addAll(main.getResources().getSrcDirs());
		}

		project.getTasks().withType(JavaExec.class, new Action<JavaExec>() {

			@Override
			public void execute(JavaExec exec) {
				ArrayList<File> files = new ArrayList<File>(exec.getClasspath()
						.getFiles());
				files.addAll(0, allResources);
				getLogger().info("Adding classpath: " + allResources);
				exec.setClasspath(new SimpleFileCollection(files));
				if (exec.getMain() == null) {
					final String mainClass = findMainClass(main);
					exec.setMain(mainClass);
					exec.getConventionMapping().map("main", new Callable<String>() {

						@Override
						public String call() throws Exception {
							return mainClass;
						}

					});
					getLogger().info("Found main: " + mainClass);
				}
				if (outputDir != null) {
					for (File directory : allResources) {
						FileUtils.removeDuplicatesFromCopy(outputDir, directory);
					}
				}
				exec.exec();
			}

		});

	}

	private String findMainClass(SourceSet main) {
		if (main == null) {
			return null;
		}
		getLogger().info("Looking for main in: " + main.getOutput().getClassesDir());
		try {
			return MainClassFinder.findMainClass(main.getOutput().getClassesDir());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot find main class", ex);
		}
	}

}
