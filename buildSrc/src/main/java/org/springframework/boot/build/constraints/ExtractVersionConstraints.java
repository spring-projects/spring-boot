/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.build.constraints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.Task;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.DependencyConstraintMetadata;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.platform.base.Platform;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library;

/**
 * {@link Task} to extract constraints from a {@link Platform}. The platform's own
 * constraints and those in any boms upon which it depends are extracted.
 *
 * @author Andy Wilkinson
 */
public class ExtractVersionConstraints extends AbstractTask {

	private final Configuration configuration;

	private final Map<String, String> versionConstraints = new TreeMap<>();

	private final Set<ConstrainedVersion> constrainedVersions = new TreeSet<>();

	private final Set<VersionProperty> versionProperties = new TreeSet<>();

	private final List<String> projectPaths = new ArrayList<>();

	public ExtractVersionConstraints() {
		DependencyHandler dependencies = getProject().getDependencies();
		this.configuration = getProject().getConfigurations().create(getName());
		dependencies.getComponents().all(this::processMetadataDetails);
	}

	public void enforcedPlatform(String projectPath) {
		this.configuration.getDependencies().add(getProject().getDependencies().enforcedPlatform(
				getProject().getDependencies().project(Collections.singletonMap("path", projectPath))));
		this.projectPaths.add(projectPath);
	}

	@Internal
	public Map<String, String> getVersionConstraints() {
		return Collections.unmodifiableMap(this.versionConstraints);
	}

	@Internal
	public Set<ConstrainedVersion> getConstrainedVersions() {
		return this.constrainedVersions;
	}

	@Internal
	public Set<VersionProperty> getVersionProperties() {
		return this.versionProperties;
	}

	@TaskAction
	void extractVersionConstraints() {
		this.configuration.resolve();
		for (String projectPath : this.projectPaths) {
			extractVersionProperties(projectPath);
			for (DependencyConstraint constraint : getProject().project(projectPath).getConfigurations()
					.getByName("apiElements").getAllDependencyConstraints()) {
				this.versionConstraints.put(constraint.getGroup() + ":" + constraint.getName(),
						constraint.getVersionConstraint().toString());
				this.constrainedVersions.add(new ConstrainedVersion(constraint.getGroup(), constraint.getName(),
						constraint.getVersionConstraint().toString()));
			}
		}
	}

	private void extractVersionProperties(String projectPath) {
		Object bom = getProject().project(projectPath).getExtensions().getByName("bom");
		BomExtension bomExtension = (BomExtension) bom;
		for (Library lib : bomExtension.getLibraries()) {
			this.versionProperties.add(new VersionProperty(lib.getName(), lib.getVersionProperty()));
		}
	}

	private void processMetadataDetails(ComponentMetadataDetails details) {
		details.allVariants((variantMetadata) -> variantMetadata.withDependencyConstraints((dependencyConstraints) -> {
			for (DependencyConstraintMetadata constraint : dependencyConstraints) {
				this.versionConstraints.put(constraint.getGroup() + ":" + constraint.getName(),
						constraint.getVersionConstraint().toString());
				this.constrainedVersions.add(new ConstrainedVersion(constraint.getGroup(), constraint.getName(),
						constraint.getVersionConstraint().toString()));
			}
		}));
	}

	public static final class ConstrainedVersion implements Comparable<ConstrainedVersion>, Serializable {

		private final String group;

		private final String artifact;

		private final String version;

		private ConstrainedVersion(String group, String artifact, String version) {
			this.group = group;
			this.artifact = artifact;
			this.version = version;
		}

		public String getGroup() {
			return this.group;
		}

		public String getArtifact() {
			return this.artifact;
		}

		public String getVersion() {
			return this.version;
		}

		@Override
		public int compareTo(ConstrainedVersion other) {
			int groupComparison = this.group.compareTo(other.group);
			if (groupComparison != 0) {
				return groupComparison;
			}
			return this.artifact.compareTo(other.artifact);
		}

	}

	public static final class VersionProperty implements Comparable<VersionProperty>, Serializable {

		private final String libraryName;

		private final String versionProperty;

		public VersionProperty(String libraryName, String versionProperty) {
			this.libraryName = libraryName;
			this.versionProperty = versionProperty;
		}

		public String getLibraryName() {
			return this.libraryName;
		}

		public String getVersionProperty() {
			return this.versionProperty;
		}

		@Override
		public int compareTo(VersionProperty other) {
			int groupComparison = this.libraryName.compareToIgnoreCase(other.libraryName);
			if (groupComparison != 0) {
				return groupComparison;
			}
			return this.versionProperty.compareTo(other.versionProperty);
		}

	}

}
