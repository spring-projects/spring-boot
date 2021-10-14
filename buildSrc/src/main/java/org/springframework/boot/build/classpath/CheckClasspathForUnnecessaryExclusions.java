/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link Task} for checking the classpath for unnecessary exclusions.
 *
 * @author Andy Wilkinson
 */
public class CheckClasspathForUnnecessaryExclusions extends DefaultTask {

	private final Map<String, Set<String>> exclusionsByDependencyId = new TreeMap<>();

	private final Map<String, Dependency> dependencyById = new HashMap<>();

	private final Dependency platform;

	private final DependencyHandler dependencyHandler;

	private final ConfigurationContainer configurations;

	@Inject
	public CheckClasspathForUnnecessaryExclusions(DependencyHandler dependencyHandler,
			ConfigurationContainer configurations) {
		this.dependencyHandler = getProject().getDependencies();
		this.configurations = getProject().getConfigurations();
		this.platform = this.dependencyHandler.create(this.dependencyHandler.platform(this.dependencyHandler
				.project(Collections.singletonMap("path", ":spring-boot-project:spring-boot-dependencies"))));
		getOutputs().upToDateWhen((task) -> true);
	}

	public void setClasspath(Configuration classpath) {
		this.exclusionsByDependencyId.clear();
		this.dependencyById.clear();
		classpath.getAllDependencies().all((dependency) -> {
			if (dependency instanceof ModuleDependency) {
				String dependencyId = dependency.getGroup() + ":" + dependency.getName();
				Set<ExcludeRule> excludeRules = ((ModuleDependency) dependency).getExcludeRules();
				TreeSet<String> exclusions = excludeRules.stream()
						.map((rule) -> rule.getGroup() + ":" + rule.getModule())
						.collect(Collectors.toCollection(TreeSet::new));
				this.exclusionsByDependencyId.put(dependencyId, exclusions);
				if (!exclusions.isEmpty()) {
					this.dependencyById.put(dependencyId, getProject().getDependencies().create(dependencyId));
				}
			}
		});
	}

	@Input
	Map<String, Set<String>> getExclusionsByDependencyId() {
		return this.exclusionsByDependencyId;
	}

	@TaskAction
	public void checkForUnnecessaryExclusions() {
		Map<String, Set<String>> unnecessaryExclusions = new HashMap<>();
		for (Entry<String, Set<String>> entry : this.exclusionsByDependencyId.entrySet()) {
			String dependencyId = entry.getKey();
			Set<String> exclusions = entry.getValue();
			if (!exclusions.isEmpty()) {
				Dependency toCheck = this.dependencyById.get(dependencyId);
				List<String> dependencies = this.configurations.detachedConfiguration(toCheck, this.platform)
						.getIncoming().getArtifacts().getArtifacts().stream().map((artifact) -> {
							ModuleComponentIdentifier id = (ModuleComponentIdentifier) artifact.getId()
									.getComponentIdentifier();
							return id.getGroup() + ":" + id.getModule();
						}).collect(Collectors.toList());
				exclusions.removeAll(dependencies);
				if (!exclusions.isEmpty()) {
					unnecessaryExclusions.put(dependencyId, exclusions);
				}
			}
		}
		if (!unnecessaryExclusions.isEmpty()) {
			StringBuilder message = new StringBuilder("Unnecessary exclusions detected:");
			for (Entry<String, Set<String>> entry : unnecessaryExclusions.entrySet()) {
				message.append(String.format("%n    %s", entry.getKey()));
				for (String exclusion : entry.getValue()) {
					message.append(String.format("%n       %s", exclusion));
				}
			}
			throw new GradleException(message.toString());
		}
	}

}
