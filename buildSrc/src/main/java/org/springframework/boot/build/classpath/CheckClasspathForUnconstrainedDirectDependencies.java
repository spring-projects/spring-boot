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

	/**
	 * Checks the classpath for unconstrained direct dependencies.
	 * @return void
	 */
	public CheckClasspathForUnconstrainedDirectDependencies() {
		getOutputs().upToDateWhen((task) -> true);
	}

	/**
	 * Returns the classpath of the CheckClasspathForUnconstrainedDirectDependencies
	 * class.
	 * @return the classpath of the CheckClasspathForUnconstrainedDirectDependencies class
	 */
	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	/**
	 * Sets the classpath for the CheckClasspathForUnconstrainedDirectDependencies class.
	 * @param classpath the Configuration object representing the classpath to be set
	 */
	public void setClasspath(Configuration classpath) {
		this.classpath = classpath;
	}

	/**
	 * Checks for unconstrained direct dependencies in the classpath.
	 *
	 * This method retrieves the resolution result from the classpath and checks for any
	 * unconstrained direct dependencies. It collects the requested dependencies and
	 * constraints, and then removes the constraints from the unconstrained dependencies.
	 * If any unconstrained dependencies are found, a GradleException is thrown with the
	 * list of unconstrained dependencies.
	 */
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
