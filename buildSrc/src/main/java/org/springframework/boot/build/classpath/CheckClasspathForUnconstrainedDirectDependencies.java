/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.boot.build.classpath;

import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.TaskAction;

/**
 * Tasks to check that none of classpath's direct dependencies are unconstrained.
 *
 * @author Andy Wilkinson
 */
public class CheckClasspathForUnconstrainedDirectDependencies extends DefaultTask {

	private Configuration classpath;

	public CheckClasspathForUnconstrainedDirectDependencies() {
		getOutputs().upToDateWhen((task) -> true);
	}

	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	public void setClasspath(Configuration classpath) {
		this.classpath = classpath;
	}

	@TaskAction
	void checkForUnconstrainedDirectDependencies() {
		ResolutionResult resolutionResult = this.classpath.getIncoming().getResolutionResult();
		Set<? extends DependencyResult> dependencies = resolutionResult.getRoot().getDependencies();
		Set<String> unconstrainedDependencies = dependencies.stream()
			.map(DependencyResult::getRequested)
			.filter(ModuleComponentSelector.class::isInstance)
			.map(ModuleComponentSelector.class::cast)
			.map((selector) -> selector.getGroup() + ":" + selector.getModule())
			.collect(Collectors.toSet());
		Set<String> constraints = resolutionResult.getAllDependencies()
			.stream()
			.filter(DependencyResult::isConstraint)
			.map(DependencyResult::getRequested)
			.filter(ModuleComponentSelector.class::isInstance)
			.map(ModuleComponentSelector.class::cast)
			.map((selector) -> selector.getGroup() + ":" + selector.getModule())
			.collect(Collectors.toSet());
		unconstrainedDependencies.removeAll(constraints);
		if (!unconstrainedDependencies.isEmpty()) {
			throw new GradleException("Found unconstrained direct dependencies: " + unconstrainedDependencies);
		}
	}

}
