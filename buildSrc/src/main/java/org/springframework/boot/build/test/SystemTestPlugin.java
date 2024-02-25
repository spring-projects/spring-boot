/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.build.test;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

/**
 * A {@link Plugin} to configure system testing support in a {@link Project}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class SystemTestPlugin implements Plugin<Project> {

	private static final Spec<Task> NEVER = (task) -> false;

	/**
	 * Name of the {@code systemTest} task.
	 */
	public static String SYSTEM_TEST_TASK_NAME = "systemTest";

	/**
	 * Name of the {@code systemTest} source set.
	 */
	public static String SYSTEM_TEST_SOURCE_SET_NAME = "systemTest";

	/**
	 * Applies the SystemTestPlugin to the given project. This plugin is applied only if
	 * the project has the JavaPlugin applied. It configures the system testing for the
	 * project.
	 * @param project the project to apply the plugin to
	 */
	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> configureSystemTesting(project));
	}

	/**
	 * Configures the system testing for the given project.
	 * @param project The project to configure system testing for.
	 */
	private void configureSystemTesting(Project project) {
		SourceSet systemTestSourceSet = createSourceSet(project);
		createTestTask(project, systemTestSourceSet);
		project.getPlugins().withType(EclipsePlugin.class, (eclipsePlugin) -> {
			EclipseModel eclipse = project.getExtensions().getByType(EclipseModel.class);
			eclipse.classpath((classpath) -> classpath.getPlusConfigurations()
				.add(project.getConfigurations()
					.getByName(systemTestSourceSet.getRuntimeClasspathConfigurationName())));
		});
	}

	/**
	 * Creates a new source set for system tests.
	 * @param project the project to create the source set for
	 * @return the created source set for system tests
	 */
	private SourceSet createSourceSet(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
		SourceSet systemTestSourceSet = sourceSets.create(SYSTEM_TEST_SOURCE_SET_NAME);
		SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		systemTestSourceSet
			.setCompileClasspath(systemTestSourceSet.getCompileClasspath().plus(mainSourceSet.getOutput()));
		systemTestSourceSet
			.setRuntimeClasspath(systemTestSourceSet.getRuntimeClasspath().plus(mainSourceSet.getOutput()));
		return systemTestSourceSet;
	}

	/**
	 * Creates a test task for running system tests.
	 * @param project The project to create the test task for.
	 * @param systemTestSourceSet The source set containing the system test classes.
	 */
	private void createTestTask(Project project, SourceSet systemTestSourceSet) {
		Test systemTest = project.getTasks().create(SYSTEM_TEST_TASK_NAME, Test.class);
		systemTest.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
		systemTest.setDescription("Runs system tests.");
		systemTest.setTestClassesDirs(systemTestSourceSet.getOutput().getClassesDirs());
		systemTest.setClasspath(systemTestSourceSet.getRuntimeClasspath());
		systemTest.shouldRunAfter(JavaPlugin.TEST_TASK_NAME);
		if (isCi()) {
			systemTest.getOutputs().upToDateWhen(NEVER);
			systemTest.getOutputs().doNotCacheIf("System tests are always rerun on CI", (task) -> true);
		}
	}

	/**
	 * Checks if the current environment is a continuous integration (CI) environment.
	 * @return true if the current environment is a CI environment, false otherwise.
	 */
	private boolean isCi() {
		return Boolean.parseBoolean(System.getenv("CI"));
	}

}
