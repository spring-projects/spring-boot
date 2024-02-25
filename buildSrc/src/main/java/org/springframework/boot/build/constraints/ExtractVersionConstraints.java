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

package org.springframework.boot.build.constraints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.DependencyConstraintMetadata;
import org.gradle.api.artifacts.dsl.DependencyHandler;
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
public class ExtractVersionConstraints extends DefaultTask {

	private final Configuration configuration;

	private final Map<String, String> versionConstraints = new TreeMap<>();

	private final Set<ConstrainedVersion> constrainedVersions = new TreeSet<>();

	private final Set<VersionProperty> versionProperties = new TreeSet<>();

	private final List<String> projectPaths = new ArrayList<>();

	/**
     * Extracts version constraints from the project's dependencies.
     * 
     * This method creates a new configuration and processes the metadata details of all components in the project's dependencies.
     * The version constraints are extracted and stored in the configuration for further use.
     */
    public ExtractVersionConstraints() {
		DependencyHandler dependencies = getProject().getDependencies();
		this.configuration = getProject().getConfigurations().create(getName());
		dependencies.getComponents().all(this::processMetadataDetails);
	}

	/**
     * Adds the enforced platform for the specified project path.
     * 
     * @param projectPath the path of the project
     */
    public void enforcedPlatform(String projectPath) {
		this.configuration.getDependencies()
			.add(getProject().getDependencies()
				.enforcedPlatform(
						getProject().getDependencies().project(Collections.singletonMap("path", projectPath))));
		this.projectPaths.add(projectPath);
	}

	/**
     * Returns an unmodifiable map of version constraints.
     *
     * @return the version constraints as an unmodifiable map
     */
    @Internal
	public Map<String, String> getVersionConstraints() {
		return Collections.unmodifiableMap(this.versionConstraints);
	}

	/**
     * Returns the set of constrained versions.
     *
     * @return the set of constrained versions
     */
    @Internal
	public Set<ConstrainedVersion> getConstrainedVersions() {
		return this.constrainedVersions;
	}

	/**
     * Returns the set of version properties.
     *
     * @return the set of version properties
     */
    @Internal
	public Set<VersionProperty> getVersionProperties() {
		return this.versionProperties;
	}

	/**
     * Extracts version constraints from the project's configurations.
     * Resolves the project's configuration before extracting the version constraints.
     * Iterates through each project path and extracts version properties.
     * Retrieves the dependency constraints from the "apiElements" configuration of each project.
     * Stores the version constraints in a map, using the group and name of the constraint as the key,
     * and the string representation of the version constraint as the value.
     * Adds each version constraint to a list of constrained versions.
     */
    @TaskAction
	void extractVersionConstraints() {
		this.configuration.resolve();
		for (String projectPath : this.projectPaths) {
			extractVersionProperties(projectPath);
			for (DependencyConstraint constraint : getProject().project(projectPath)
				.getConfigurations()
				.getByName("apiElements")
				.getAllDependencyConstraints()) {
				this.versionConstraints.put(constraint.getGroup() + ":" + constraint.getName(),
						constraint.getVersionConstraint().toString());
				this.constrainedVersions.add(new ConstrainedVersion(constraint.getGroup(), constraint.getName(),
						constraint.getVersionConstraint().toString()));
			}
		}
	}

	/**
     * Extracts the version properties from the specified project path.
     * 
     * @param projectPath the path of the project to extract version properties from
     */
    private void extractVersionProperties(String projectPath) {
		Object bom = getProject().project(projectPath).getExtensions().getByName("bom");
		BomExtension bomExtension = (BomExtension) bom;
		for (Library lib : bomExtension.getLibraries()) {
			String versionProperty = lib.getVersionProperty();
			if (versionProperty != null) {
				this.versionProperties.add(new VersionProperty(lib.getName(), versionProperty));
			}
		}
	}

	/**
     * Processes the metadata details of a component.
     * 
     * @param details the component metadata details to be processed
     */
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

	/**
     * ConstrainedVersion class.
     */
    public static final class ConstrainedVersion implements Comparable<ConstrainedVersion>, Serializable {

		private final String group;

		private final String artifact;

		private final String version;

		/**
         * Constructs a new instance of the ConstrainedVersion class with the specified group, artifact, and version.
         * 
         * @param group the group of the version
         * @param artifact the artifact of the version
         * @param version the version string
         */
        private ConstrainedVersion(String group, String artifact, String version) {
			this.group = group;
			this.artifact = artifact;
			this.version = version;
		}

		/**
         * Returns the group of the ConstrainedVersion object.
         * 
         * @return the group of the ConstrainedVersion object
         */
        public String getGroup() {
			return this.group;
		}

		/**
         * Returns the artifact of the ConstrainedVersion.
         *
         * @return the artifact of the ConstrainedVersion
         */
        public String getArtifact() {
			return this.artifact;
		}

		/**
         * Returns the version of the ConstrainedVersion object.
         *
         * @return the version of the ConstrainedVersion object
         */
        public String getVersion() {
			return this.version;
		}

		/**
         * Compares this ConstrainedVersion object with the specified ConstrainedVersion object for order.
         * 
         * @param other the ConstrainedVersion object to be compared
         * @return a negative integer, zero, or a positive integer as this ConstrainedVersion object is less than, equal to, or greater than the specified ConstrainedVersion object
         * @throws NullPointerException if the specified ConstrainedVersion object is null
         */
        @Override
		public int compareTo(ConstrainedVersion other) {
			int groupComparison = this.group.compareTo(other.group);
			if (groupComparison != 0) {
				return groupComparison;
			}
			return this.artifact.compareTo(other.artifact);
		}

	}

	/**
     * VersionProperty class.
     */
    public static final class VersionProperty implements Comparable<VersionProperty>, Serializable {

		private final String libraryName;

		private final String versionProperty;

		/**
         * Constructs a new VersionProperty object with the specified library name and version property.
         * 
         * @param libraryName the name of the library
         * @param versionProperty the version property of the library
         */
        public VersionProperty(String libraryName, String versionProperty) {
			this.libraryName = libraryName;
			this.versionProperty = versionProperty;
		}

		/**
         * Returns the name of the library.
         *
         * @return the name of the library
         */
        public String getLibraryName() {
			return this.libraryName;
		}

		/**
         * Returns the version property.
         *
         * @return the version property
         */
        public String getVersionProperty() {
			return this.versionProperty;
		}

		/**
         * Compares this VersionProperty object with the specified VersionProperty object for order.
         * 
         * @param other the VersionProperty object to be compared
         * @return a negative integer, zero, or a positive integer as this VersionProperty object is less than, equal to, or greater than the specified VersionProperty object
         * @throws NullPointerException if the specified VersionProperty object is null
         */
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
