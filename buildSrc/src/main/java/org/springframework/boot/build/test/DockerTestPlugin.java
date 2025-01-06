/*
 * Copyright 2012-2024 the original author or authors.
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
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

/**
 * Plugin for Docker-based tests. Creates a {@link SourceSet source set}, {@link Test
 * test} task, and {@link BuildService shared service} named {@code dockerTest}. The build
 * service is configured to only allow serial usage and the {@code dockerTest} task is
 * configured to use the build service. In a parallel build, this ensures that only a
 * single {@code dockerTest} task can run at any given time.
 *
 * @author Andy Wilkinson
 */
public class DockerTestPlugin implements Plugin<Project> {

	/**
	 * Name of the {@code dockerTest} task.
	 */
	public static final String DOCKER_TEST_TASK_NAME = "dockerTest";

	/**
	 * Name of the {@code dockerTest} source set.
	 */
	public static final String DOCKER_TEST_SOURCE_SET_NAME = "dockerTest";

	/**
	 * Name of the {@code dockerTest} shared service.
	 */
	public static final String DOCKER_TEST_SERVICE_NAME = "dockerTest";

	private static final String RECLAIM_DOCKER_SPACE_TASK_NAME = "reclaimDockerSpace";

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> configureDockerTesting(project));
	}

	private void configureDockerTesting(Project project) {
		Provider<DockerTestBuildService> buildService = DockerTestBuildService.registerIfNecessary(project);
		SourceSet dockerTestSourceSet = createSourceSet(project);
		Provider<Test> dockerTest = createTestTask(project, dockerTestSourceSet, buildService);
		project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(dockerTest);
		project.getPlugins().withType(EclipsePlugin.class, (eclipsePlugin) -> {
			EclipseModel eclipse = project.getExtensions().getByType(EclipseModel.class);
			eclipse.classpath((classpath) -> classpath.getPlusConfigurations()
				.add(project.getConfigurations()
					.getByName(dockerTestSourceSet.getRuntimeClasspathConfigurationName())));
		});
		project.getDependencies()
			.add(dockerTestSourceSet.getRuntimeOnlyConfigurationName(), "org.junit.platform:junit-platform-launcher");
		Provider<Exec> reclaimDockerSpace = createReclaimDockerSpaceTask(project, buildService);
		project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(reclaimDockerSpace);
	}

	private SourceSet createSourceSet(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
		SourceSet dockerTestSourceSet = sourceSets.create(DOCKER_TEST_SOURCE_SET_NAME);
		SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
		dockerTestSourceSet.setCompileClasspath(dockerTestSourceSet.getCompileClasspath()
			.plus(main.getOutput())
			.plus(main.getCompileClasspath())
			.plus(test.getOutput()));
		dockerTestSourceSet.setRuntimeClasspath(dockerTestSourceSet.getRuntimeClasspath()
			.plus(main.getOutput())
			.plus(main.getRuntimeClasspath())
			.plus(test.getOutput()));
		project.getPlugins().withType(IntegrationTestPlugin.class, (integrationTestPlugin) -> {
			SourceSet intTest = sourceSets.getByName(IntegrationTestPlugin.INT_TEST_SOURCE_SET_NAME);
			dockerTestSourceSet
				.setCompileClasspath(dockerTestSourceSet.getCompileClasspath().plus(intTest.getOutput()));
			dockerTestSourceSet
				.setRuntimeClasspath(dockerTestSourceSet.getRuntimeClasspath().plus(intTest.getOutput()));
		});
		return dockerTestSourceSet;
	}

	private Provider<Test> createTestTask(Project project, SourceSet dockerTestSourceSet,
			Provider<DockerTestBuildService> buildService) {
		return project.getTasks().register(DOCKER_TEST_TASK_NAME, Test.class, (task) -> {
			task.usesService(buildService);
			task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
			task.setDescription("Runs Docker-based tests.");
			task.setTestClassesDirs(dockerTestSourceSet.getOutput().getClassesDirs());
			task.setClasspath(dockerTestSourceSet.getRuntimeClasspath());
			task.shouldRunAfter(JavaPlugin.TEST_TASK_NAME);
		});
	}

	private Provider<Exec> createReclaimDockerSpaceTask(Project project,
			Provider<DockerTestBuildService> buildService) {
		return project.getTasks().register(RECLAIM_DOCKER_SPACE_TASK_NAME, Exec.class, (task) -> {
			task.usesService(buildService);
			task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
			task.setDescription("Reclaims Docker space on CI.");
			task.shouldRunAfter(DOCKER_TEST_TASK_NAME);
			task.onlyIf(this::shouldReclaimDockerSpace);
			task.executable("bash");
			task.args("-c",
					project.getRootDir()
						.toPath()
						.resolve(".github/scripts/reclaim-docker-diskspace.sh")
						.toAbsolutePath());
		});
	}

	private boolean shouldReclaimDockerSpace(Task task) {
		if (System.getProperty("os.name").startsWith("Windows")) {
			return false;
		}
		return System.getenv("GITHUB_ACTIONS") != null || System.getenv("RECLAIM_DOCKER_SPACE") != null;
	}

}
