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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Add a main class if one is missing from the build
 * 
 * @author Dave Syer
 */
public class ComputeMain implements Action<Task> {

	private Project project;

	public ComputeMain(Project project) {
		this.project = project;
	}

	@Override
	public void execute(Task task) {
		if (task instanceof JavaExec) {
			final JavaExec exec = (JavaExec) task;
			this.project.afterEvaluate(new Action<Project>() {
				@Override
				public void execute(Project project) {
					addMain(exec);
				}
			});
		}
	}

	private void addMain(JavaExec exec) {
		if (exec.getMain() == null) {
			this.project.getLogger().debug("Computing main for: " + exec);
			this.project.setProperty("mainClassName", findMainClass(this.project));
		}
	}

	private String findMainClass(Project project) {
		SourceSet main = findMainSourceSet(project);
		if (main == null) {
			return null;
		}
		project.getLogger().debug(
				"Looking for main in: " + main.getOutput().getClassesDir());
		try {
			String mainClass = MainClassFinder.findMainClass(main.getOutput()
					.getClassesDir());
			project.getLogger().info("Computed main class: " + mainClass);
			return mainClass;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot find main class", ex);
		}
	}

	public static SourceSet findMainSourceSet(Project project) {
		final AtomicReference<SourceSet> main = new AtomicReference<SourceSet>();
		JavaPluginConvention javaConvention = project.getConvention().getPlugin(
				JavaPluginConvention.class);
		javaConvention.getSourceSets().all(new Action<SourceSet>() {

			@Override
			public void execute(SourceSet set) {
				if (SourceSet.MAIN_SOURCE_SET_NAME.equals(set.getName())) {
					main.set(set);
				}
			};

		});
		return main.get();
	}
}
