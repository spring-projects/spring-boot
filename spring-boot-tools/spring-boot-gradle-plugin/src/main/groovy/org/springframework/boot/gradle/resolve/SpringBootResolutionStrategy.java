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

package org.springframework.boot.gradle.resolve;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.springframework.boot.dependency.tools.Dependency;
import org.springframework.boot.dependency.tools.ManagedDependencies;
import org.springframework.boot.gradle.VersionManagedDependencies;

/**
 * A resolution strategy to resolve missing version numbers using the
 * 'spring-boot-dependencies' POM.
 *
 * @author Phillip Webb
 */
public class SpringBootResolutionStrategy {

	private static final String SPRING_BOOT_GROUP = "org.springframework.boot";

	public static void applyToConfiguration(final Project project,
			Configuration configuration) {
		if (VersionManagedDependencies.CONFIGURATION.equals(configuration.getName())) {
			return;
		}
		VersionResolver versionResolver = new VersionResolver(project);
		configuration.getResolutionStrategy().eachDependency(versionResolver);
	}

	private static class VersionResolver implements Action<DependencyResolveDetails> {

		private final VersionManagedDependencies versionManagedDependencies;

		public VersionResolver(Project project) {
			this.versionManagedDependencies = new VersionManagedDependencies(project);
		}

		@Override
		public void execute(DependencyResolveDetails resolveDetails) {
			String version = resolveDetails.getTarget().getVersion();
			if (version == null || version.trim().length() == 0) {
				resolve(resolveDetails);
			}
		}

		private void resolve(DependencyResolveDetails resolveDetails) {
			ManagedDependencies dependencies = this.versionManagedDependencies
					.getManagedDependencies();
			ModuleVersionSelector target = resolveDetails.getTarget();
			if (SPRING_BOOT_GROUP.equals(target.getGroup())) {
				resolveDetails.useVersion(dependencies.getSpringBootVersion());
				return;
			}
			Dependency dependency = dependencies
					.find(target.getGroup(), target.getName());
			if (dependency != null) {
				resolveDetails.useVersion(dependency.getVersion());
			}
		}

	}
}
