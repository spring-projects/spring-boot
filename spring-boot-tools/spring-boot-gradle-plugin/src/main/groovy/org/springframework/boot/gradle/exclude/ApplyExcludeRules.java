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

package org.springframework.boot.gradle.exclude;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.internal.artifacts.DefaultExcludeRule;
import org.gradle.api.logging.Logger;
import org.springframework.boot.dependency.tools.Dependency.Exclusion;
import org.springframework.boot.dependency.tools.ManagedDependencies;
import org.springframework.boot.gradle.VersionManagedDependencies;

/**
 * {@link Action} to apply exclude rules.
 *
 * @author Phillip Webb
 */
public class ApplyExcludeRules implements Action<Configuration> {

	private final Logger logger;

	private final VersionManagedDependencies versionManagedDependencies;

	public ApplyExcludeRules(Project project) {
		this.logger = project.getLogger();
		this.versionManagedDependencies = new VersionManagedDependencies(project);
	}

	@Override
	public void execute(Configuration configuration) {
		if (!VersionManagedDependencies.CONFIGURATION.equals(configuration.getName())) {
			configuration.getIncoming().beforeResolve(
					new Action<ResolvableDependencies>() {
						@Override
						public void execute(ResolvableDependencies resolvableDependencies) {
							resolvableDependencies.getDependencies().all(
									new Action<Dependency>() {
										@Override
										public void execute(Dependency dependency) {
											applyExcludeRules(dependency);
										}
									});
						}
					});
		}
	}

	private void applyExcludeRules(Dependency dependency) {
		if (dependency instanceof ModuleDependency) {
			applyExcludeRules((ModuleDependency) dependency);
		}
	}

	private void applyExcludeRules(ModuleDependency dependency) {
		ManagedDependencies managedDependencies = versionManagedDependencies
				.getManagedDependencies();
		// flat directory repositories do not have groups
		if (dependency.getGroup() != null) {
			org.springframework.boot.dependency.tools.Dependency managedDependency = managedDependencies
					.find(dependency.getGroup(), dependency.getName());
			if (managedDependency != null) {
				for (Exclusion exclusion : managedDependency.getExclusions()) {
					addExcludeRule(dependency, exclusion);
				}
				addImplicitExcludeRules(dependency);
				return;
			}
		}
		logger.debug("No exclusions rules applied for non-managed dependency "
				+ dependency);
	}

	private void addExcludeRule(ModuleDependency dependency, Exclusion exclusion) {
		logger.info("Adding managed exclusion rule " + exclusion + " to " + dependency);
		DefaultExcludeRule rule = new DefaultExcludeRule(exclusion.getGroupId(),
				exclusion.getArtifactId());
		dependency.getExcludeRules().add(rule);
	}

	private void addImplicitExcludeRules(ModuleDependency dependency) {
		if (isStarter(dependency)) {
			logger.info("Adding implicit managed exclusion rules to starter "
					+ dependency);
			dependency.getExcludeRules().add(
					new DefaultExcludeRule("commons-logging", "commons-logging"));
			dependency.getExcludeRules().add(
					new DefaultExcludeRule("commons-logging", "commons-logging-api"));
		}
	}

	private boolean isStarter(ModuleDependency dependency) {
		return (dependency.getGroup() != null
				&& dependency.getGroup().equals("org.springframework.boot") && dependency
				.getName().startsWith("spring-boot-starter"));
	}

}
