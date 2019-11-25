/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.UnresolvedDependency;

/**
 * An analyzer for {@link UnresolvedDependency unresolvable dependencies} that logs a
 * warning suggesting that the {@code io.spring.dependency-management} plugin is applied
 * when one or more versionless dependencies fails to resolve.
 *
 * @author Andy Wilkinson
 */
class UnresolvedDependenciesAnalyzer {

	private static final Log logger = LogFactory.getLog(SpringBootPlugin.class);

	private Set<ModuleVersionSelector> dependenciesWithNoVersion = new HashSet<>();

	void analyze(Set<UnresolvedDependency> unresolvedDependencies) {
		this.dependenciesWithNoVersion = unresolvedDependencies.stream()
				.map((unresolvedDependency) -> unresolvedDependency.getSelector()).filter(this::hasNoVersion)
				.collect(Collectors.toSet());
	}

	void buildFinished(Project project) {
		if (!this.dependenciesWithNoVersion.isEmpty()
				&& !project.getPlugins().hasPlugin(DependencyManagementPlugin.class)) {
			StringBuilder message = new StringBuilder();
			message.append("\nDuring the build, one or more dependencies that were "
					+ "declared without a version failed to resolve:\n");
			this.dependenciesWithNoVersion
					.forEach((dependency) -> message.append("    ").append(dependency).append("\n"));
			message.append("\nDid you forget to apply the io.spring.dependency-management plugin to the ");
			message.append(project.getName()).append(" project?\n");
			logger.warn(message.toString());
		}
	}

	private boolean hasNoVersion(ModuleVersionSelector selector) {
		String version = selector.getVersion();
		return version == null || version.trim().isEmpty();
	}

}
