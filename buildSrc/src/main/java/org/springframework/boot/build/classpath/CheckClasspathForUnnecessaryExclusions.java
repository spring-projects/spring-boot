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

package org.springframework.boot.build.classpath;

import java.util.Collections;
import java.util.HashMap;
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
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link Task} for checking the classpath for unnecessary exclusions.
 *
 * @author Andy Wilkinson
 */
public class CheckClasspathForUnnecessaryExclusions extends DefaultTask {

	private static final Map<String, String> SPRING_BOOT_DEPENDENCIES_PROJECT = Collections.singletonMap("path",
			":spring-boot-project:spring-boot-dependencies");

	private final Map<String, Set<String>> exclusionsByDependencyId = new TreeMap<>();

	private final Map<String, Dependency> dependencyById = new HashMap<>();

	private final Dependency platform;

	private final DependencyHandler dependencyHandler;

	private final ConfigurationContainer configurations;

	private Configuration classpath;

	/**
	 * Constructor for the CheckClasspathForUnnecessaryExclusions class.
	 * @param dependencyHandler The dependency handler used to manage dependencies.
	 * @param configurations The container for project configurations.
	 */
	@Inject
	public CheckClasspathForUnnecessaryExclusions(DependencyHandler dependencyHandler,
			ConfigurationContainer configurations) {
		this.dependencyHandler = getProject().getDependencies();
		this.configurations = getProject().getConfigurations();
		this.platform = this.dependencyHandler
			.create(this.dependencyHandler.platform(this.dependencyHandler.project(SPRING_BOOT_DEPENDENCIES_PROJECT)));
		getOutputs().upToDateWhen((task) -> true);
	}

	/**
	 * Sets the classpath configuration.
	 * @param classpath the classpath configuration to set
	 */
	public void setClasspath(Configuration classpath) {
		this.classpath = classpath;
		this.exclusionsByDependencyId.clear();
		this.dependencyById.clear();
		classpath.getAllDependencies().all(this::processDependency);
	}

	/**
	 * Returns the classpath of the CheckClasspathForUnnecessaryExclusions class.
	 * @return the classpath of the CheckClasspathForUnnecessaryExclusions class
	 */
	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	/**
	 * Processes the given dependency.
	 * @param dependency the dependency to be processed
	 */
	private void processDependency(Dependency dependency) {
		if (dependency instanceof ModuleDependency moduleDependency) {
			processDependency(moduleDependency);
		}
	}

	/**
	 * Processes a module dependency by extracting its ID and exclude rules.
	 * @param dependency the module dependency to process
	 */
	private void processDependency(ModuleDependency dependency) {
		String dependencyId = getId(dependency);
		TreeSet<String> exclusions = dependency.getExcludeRules()
			.stream()
			.map(this::getId)
			.collect(Collectors.toCollection(TreeSet::new));
		this.exclusionsByDependencyId.put(dependencyId, exclusions);
		if (!exclusions.isEmpty()) {
			this.dependencyById.put(dependencyId, getProject().getDependencies().create(dependencyId));
		}
	}

	/**
	 * Returns a map of exclusions by dependency ID.
	 * @return the map of exclusions by dependency ID
	 */
	@Input
	Map<String, Set<String>> getExclusionsByDependencyId() {
		return this.exclusionsByDependencyId;
	}

	/**
	 * Checks for unnecessary exclusions in the classpath.
	 *
	 * This method iterates through the exclusionsByDependencyId map and checks if there
	 * are any exclusions present for each dependency. If exclusions are found, it
	 * retrieves the dependency to check and gets the incoming artifacts for the specified
	 * platform. It then removes any exclusions that match the artifact IDs. After that,
	 * it removes any profile exclusions for the dependency. If there are still exclusions
	 * remaining, it adds them to the unnecessaryExclusions map. Finally, if there are any
	 * unnecessary exclusions found, a GradleException is thrown with the exception
	 * message.
	 */
	@TaskAction
	public void checkForUnnecessaryExclusions() {
		Map<String, Set<String>> unnecessaryExclusions = new HashMap<>();
		this.exclusionsByDependencyId.forEach((dependencyId, exclusions) -> {
			if (!exclusions.isEmpty()) {
				Dependency toCheck = this.dependencyById.get(dependencyId);
				this.configurations.detachedConfiguration(toCheck, this.platform)
					.getIncoming()
					.getArtifacts()
					.getArtifacts()
					.stream()
					.map(this::getId)
					.forEach(exclusions::remove);
				removeProfileExclusions(dependencyId, exclusions);
				if (!exclusions.isEmpty()) {
					unnecessaryExclusions.put(dependencyId, exclusions);
				}
			}
		});
		if (!unnecessaryExclusions.isEmpty()) {
			throw new GradleException(getExceptionMessage(unnecessaryExclusions));
		}
	}

	/**
	 * Removes the specified exclusions from the given dependency.
	 * @param dependencyId the ID of the dependency
	 * @param exclusions the set of exclusions to be removed
	 */
	private void removeProfileExclusions(String dependencyId, Set<String> exclusions) {
		if ("org.xmlunit:xmlunit-core".equals(dependencyId)) {
			exclusions.remove("javax.xml.bind:jaxb-api");
		}
	}

	/**
	 * Returns the exception message for unnecessary exclusions.
	 * @param unnecessaryExclusions a map containing the unnecessary exclusions
	 * @return the exception message
	 */
	private String getExceptionMessage(Map<String, Set<String>> unnecessaryExclusions) {
		StringBuilder message = new StringBuilder("Unnecessary exclusions detected:");
		for (Entry<String, Set<String>> entry : unnecessaryExclusions.entrySet()) {
			message.append(String.format("%n    %s", entry.getKey()));
			for (String exclusion : entry.getValue()) {
				message.append(String.format("%n       %s", exclusion));
			}
		}
		return message.toString();
	}

	/**
	 * Returns the ID of the given artifact.
	 * @param artifact the resolved artifact result
	 * @return the ID of the artifact
	 */
	private String getId(ResolvedArtifactResult artifact) {
		return getId((ModuleComponentIdentifier) artifact.getId().getComponentIdentifier());
	}

	/**
	 * Returns the ID of the given ModuleDependency. The ID is generated by concatenating
	 * the group and name of the dependency.
	 * @param dependency the ModuleDependency to get the ID for
	 * @return the ID of the dependency
	 */
	private String getId(ModuleDependency dependency) {
		return dependency.getGroup() + ":" + dependency.getName();
	}

	/**
	 * Returns the ID of the given ExcludeRule. The ID is generated by concatenating the
	 * group and module of the rule.
	 * @param rule the ExcludeRule for which to get the ID
	 * @return the ID of the ExcludeRule
	 */
	private String getId(ExcludeRule rule) {
		return rule.getGroup() + ":" + rule.getModule();
	}

	/**
	 * Returns the ID of the given ModuleComponentIdentifier. The ID is generated by
	 * concatenating the group and module of the identifier.
	 * @param identifier the ModuleComponentIdentifier to get the ID from
	 * @return the ID of the identifier
	 */
	private String getId(ModuleComponentIdentifier identifier) {
		return identifier.getGroup() + ":" + identifier.getModule();
	}

}
