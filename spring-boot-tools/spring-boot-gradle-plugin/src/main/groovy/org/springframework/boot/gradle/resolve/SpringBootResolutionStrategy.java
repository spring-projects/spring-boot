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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.springframework.boot.dependency.tools.Dependencies;
import org.springframework.boot.dependency.tools.Dependency;
import org.springframework.boot.dependency.tools.ManagedDependencies;
import org.springframework.boot.dependency.tools.PropertiesFileDependencies;

/**
 * A resolution strategy to resolve missing version numbers using the
 * 'spring-boot-dependencies' POM.
 *
 * @author Phillip Webb
 */
public class SpringBootResolutionStrategy {

	public static final String VERSION_MANAGEMENT_CONFIGURATION = "versionManagement";

	private static final String SPRING_BOOT_GROUP = "org.springframework.boot";

	public static void applyToConfiguration(final Project project, Configuration configuration) {
		if (VERSION_MANAGEMENT_CONFIGURATION.equals(configuration.getName())) {
			return;
		}
		VersionResolver versionResolver = new VersionResolver(project);
		configuration.getResolutionStrategy().eachDependency(versionResolver);
	}

	private static class VersionResolver implements Action<DependencyResolveDetails> {

		private Configuration versionManagementConfiguration;

		private Collection<Dependencies> versionManagedDependencies;

		public VersionResolver(Project project) {
			this.versionManagementConfiguration = project.getConfigurations().getByName(
					VERSION_MANAGEMENT_CONFIGURATION);
		}

		@Override
		public void execute(DependencyResolveDetails resolveDetails) {
			String version = resolveDetails.getTarget().getVersion();
			if (version == null || version.trim().length() == 0) {
				resolve(resolveDetails);
			}
		}

		private void resolve(DependencyResolveDetails resolveDetails) {
			ManagedDependencies dependencies = ManagedDependencies.get(
					getVersionManagedDependencies());
			ModuleVersionSelector target = resolveDetails.getTarget();
			if (SPRING_BOOT_GROUP.equals(target.getGroup())) {
				resolveDetails.useVersion(dependencies.getSpringBootVersion());
				return;
			}
			Dependency dependency = dependencies.find(target.getGroup(), target.getName());
			if (dependency != null) {
				resolveDetails.useVersion(dependency.getVersion());
			}
		}

		private Collection<Dependencies> getVersionManagedDependencies() {
			if (versionManagedDependencies == null) {
				Set<File> files = versionManagementConfiguration.resolve();
				List<Dependencies> dependencies = new ArrayList<Dependencies>(
						files.size());
				for (File file : files) {
					dependencies.add(getPropertiesFileManagedDependencies(file));
				}
				this.versionManagedDependencies = dependencies;
			}
			return versionManagedDependencies;
		}

		private Dependencies getPropertiesFileManagedDependencies(File file) {
			if (!file.getName().toLowerCase().endsWith(".properties")) {
				throw new IllegalStateException(file + " is not a version property file");
			}
			try {
				return new PropertiesFileDependencies(new FileInputStream(file));
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}
}
