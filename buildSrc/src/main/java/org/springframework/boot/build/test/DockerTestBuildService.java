/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.test;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * Build service for Docker-based tests. The maximum number of {@code dockerTest} tasks
 * that can run in parallel can be configured using
 * {@code org.springframework.boot.dockertest.max-parallel-tasks}. By default, only a
 * single {@code dockerTest} task will run at a time.
 *
 * @author Andy Wilkinson
 */
abstract class DockerTestBuildService implements BuildService<BuildServiceParameters.None> {

	static Provider<DockerTestBuildService> registerIfNecessary(Project project) {
		return project.getGradle()
			.getSharedServices()
			.registerIfAbsent("dockerTest", DockerTestBuildService.class,
					(spec) -> spec.getMaxParallelUsages().set(maxParallelTasks(project)));
	}

	private static int maxParallelTasks(Project project) {
		Object property = project.findProperty("org.springframework.boot.dockertest.max-parallel-tasks");
		if (property == null) {
			return 1;
		}
		return Integer.parseInt(property.toString());
	}

}
