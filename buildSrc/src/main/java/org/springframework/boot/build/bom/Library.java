/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.build.bom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.DependencyResult;

import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * A collection of modules, Maven plugins, and Maven boms that are versioned and released
 * together.
 *
 * @author Andy Wilkinson
 */
public class Library {

	private final String name;

	private final String calendarName;

	private final LibraryVersion version;

	private final List<Group> groups;

	private final String versionProperty;

	private final List<ProhibitedVersion> prohibitedVersions;

	private final boolean considerSnapshots;

	private final VersionAlignment versionAlignment;

	/**
	 * Create a new {@code Library} with the given {@code name}, {@code version}, and
	 * {@code groups}.
	 * @param name name of the library
	 * @param calendarName name of the library as it appears in the Spring Calendar. May
	 * be {@code null} in which case the {@code name} is used.
	 * @param version version of the library
	 * @param groups groups in the library
	 * @param prohibitedVersions version of the library that are prohibited
	 * @param considerSnapshots whether to consider snapshots
	 * @param versionAlignment version alignment, if any, for the library
	 */
	public Library(String name, String calendarName, LibraryVersion version, List<Group> groups,
			List<ProhibitedVersion> prohibitedVersions, boolean considerSnapshots, VersionAlignment versionAlignment) {
		this.name = name;
		this.calendarName = (calendarName != null) ? calendarName : name;
		this.version = version;
		this.groups = groups;
		this.versionProperty = "Spring Boot".equals(name) ? null
				: name.toLowerCase(Locale.ENGLISH).replace(' ', '-') + ".version";
		this.prohibitedVersions = prohibitedVersions;
		this.considerSnapshots = considerSnapshots;
		this.versionAlignment = versionAlignment;
	}

	/**
	 * Returns the name of the Library.
	 * @return the name of the Library
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the name of the calendar.
	 * @return the name of the calendar
	 */
	public String getCalendarName() {
		return this.calendarName;
	}

	/**
	 * Returns the version of the library.
	 * @return the version of the library
	 */
	public LibraryVersion getVersion() {
		return this.version;
	}

	/**
	 * Returns the list of groups in the library.
	 * @return the list of groups
	 */
	public List<Group> getGroups() {
		return this.groups;
	}

	/**
	 * Returns the version property of the Library.
	 * @return the version property of the Library
	 */
	public String getVersionProperty() {
		return this.versionProperty;
	}

	/**
	 * Returns a list of prohibited versions.
	 * @return the list of prohibited versions
	 */
	public List<ProhibitedVersion> getProhibitedVersions() {
		return this.prohibitedVersions;
	}

	/**
	 * Returns a boolean value indicating whether snapshots should be considered.
	 * @return true if snapshots should be considered, false otherwise
	 */
	public boolean isConsiderSnapshots() {
		return this.considerSnapshots;
	}

	/**
	 * Returns the version alignment of the library.
	 * @return the version alignment of the library
	 */
	public VersionAlignment getVersionAlignment() {
		return this.versionAlignment;
	}

	/**
	 * A version or range of versions that are prohibited from being used in a bom.
	 */
	public static class ProhibitedVersion {

		private final VersionRange range;

		private final List<String> startsWith;

		private final List<String> endsWith;

		private final List<String> contains;

		private final String reason;

		/**
		 * Constructs a new ProhibitedVersion object with the specified range, startswith,
		 * endswith, contains, and reason.
		 * @param range the VersionRange object representing the range of prohibited
		 * versions
		 * @param startsWith the list of strings representing the prohibited versions that
		 * start with any of the specified strings
		 * @param endsWith the list of strings representing the prohibited versions that
		 * end with any of the specified strings
		 * @param contains the list of strings representing the prohibited versions that
		 * contain any of the specified strings
		 * @param reason the reason for prohibiting the specified versions
		 */
		public ProhibitedVersion(VersionRange range, List<String> startsWith, List<String> endsWith,
				List<String> contains, String reason) {
			this.range = range;
			this.startsWith = startsWith;
			this.endsWith = endsWith;
			this.contains = contains;
			this.reason = reason;
		}

		/**
		 * Returns the range of prohibited versions.
		 * @return the range of prohibited versions
		 */
		public VersionRange getRange() {
			return this.range;
		}

		/**
		 * Returns the list of strings that start with a specific character or substring.
		 * @return the list of strings that start with a specific character or substring
		 */
		public List<String> getStartsWith() {
			return this.startsWith;
		}

		/**
		 * Returns the list of strings representing the suffixes used for filtering.
		 * @return the list of strings representing the suffixes used for filtering
		 */
		public List<String> getEndsWith() {
			return this.endsWith;
		}

		/**
		 * Returns the list of strings containing the prohibited versions.
		 * @return the list of strings containing the prohibited versions
		 */
		public List<String> getContains() {
			return this.contains;
		}

		/**
		 * Returns the reason for the prohibition.
		 * @return the reason for the prohibition
		 */
		public String getReason() {
			return this.reason;
		}

		/**
		 * Checks if a given candidate string is prohibited.
		 * @param candidate the candidate string to check
		 * @return true if the candidate string is prohibited, false otherwise
		 */
		public boolean isProhibited(String candidate) {
			boolean result = false;
			result = result
					|| (this.range != null && this.range.containsVersion(new DefaultArtifactVersion(candidate)));
			result = result || this.startsWith.stream().anyMatch(candidate::startsWith);
			result = result || this.endsWith.stream().anyMatch(candidate::endsWith);
			result = result || this.contains.stream().anyMatch(candidate::contains);
			return result;
		}

	}

	/**
	 * LibraryVersion class.
	 */
	public static class LibraryVersion {

		private final DependencyVersion version;

		/**
		 * Constructs a new LibraryVersion object with the specified DependencyVersion.
		 * @param version the DependencyVersion to set for the LibraryVersion object
		 */
		public LibraryVersion(DependencyVersion version) {
			this.version = version;
		}

		/**
		 * Returns the version of the dependency.
		 * @return the version of the dependency
		 */
		public DependencyVersion getVersion() {
			return this.version;
		}

	}

	/**
	 * A collection of modules, Maven plugins, and Maven boms with the same group ID.
	 */
	public static class Group {

		private final String id;

		private final List<Module> modules;

		private final List<String> plugins;

		private final List<String> boms;

		/**
		 * Constructs a new Group object with the specified id, modules, plugins, and
		 * boms.
		 * @param id the id of the group
		 * @param modules the list of modules associated with the group
		 * @param plugins the list of plugins associated with the group
		 * @param boms the list of boms associated with the group
		 */
		public Group(String id, List<Module> modules, List<String> plugins, List<String> boms) {
			this.id = id;
			this.modules = modules;
			this.plugins = plugins;
			this.boms = boms;
		}

		/**
		 * Returns the ID of the group.
		 * @return the ID of the group
		 */
		public String getId() {
			return this.id;
		}

		/**
		 * Returns the list of modules in the group.
		 * @return the list of modules in the group
		 */
		public List<Module> getModules() {
			return this.modules;
		}

		/**
		 * Returns the list of plugins.
		 * @return the list of plugins
		 */
		public List<String> getPlugins() {
			return this.plugins;
		}

		/**
		 * Returns the list of BOMs.
		 * @return the list of BOMs
		 */
		public List<String> getBoms() {
			return this.boms;
		}

	}

	/**
	 * A module in a group.
	 */
	public static class Module {

		private final String name;

		private final String type;

		private final String classifier;

		private final List<Exclusion> exclusions;

		/**
		 * Constructs a new Module with the specified name.
		 * @param name the name of the module
		 */
		public Module(String name) {
			this(name, Collections.emptyList());
		}

		/**
		 * Constructs a new Module with the specified name and type.
		 * @param name the name of the module
		 * @param type the type of the module
		 */
		public Module(String name, String type) {
			this(name, type, null, Collections.emptyList());
		}

		/**
		 * Constructs a new Module with the specified name and exclusions.
		 * @param name the name of the module
		 * @param exclusions the list of exclusions for the module
		 */
		public Module(String name, List<Exclusion> exclusions) {
			this(name, null, null, exclusions);
		}

		/**
		 * Constructs a new Module with the specified name, type, classifier, and
		 * exclusions.
		 * @param name the name of the module
		 * @param type the type of the module
		 * @param classifier the classifier of the module (can be null)
		 * @param exclusions the list of exclusions for the module
		 */
		public Module(String name, String type, String classifier, List<Exclusion> exclusions) {
			this.name = name;
			this.type = type;
			this.classifier = (classifier != null) ? classifier : "";
			this.exclusions = exclusions;
		}

		/**
		 * Returns the name of the Module.
		 * @return the name of the Module
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the classifier of the Module.
		 * @return the classifier of the Module
		 */
		public String getClassifier() {
			return this.classifier;
		}

		/**
		 * Returns the type of the Module.
		 * @return the type of the Module
		 */
		public String getType() {
			return this.type;
		}

		/**
		 * Returns the list of exclusions.
		 * @return the list of exclusions
		 */
		public List<Exclusion> getExclusions() {
			return this.exclusions;
		}

	}

	/**
	 * An exclusion of a dependency identified by its group ID and artifact ID.
	 */
	public static class Exclusion {

		private final String groupId;

		private final String artifactId;

		/**
		 * Constructs a new Exclusion with the specified groupId and artifactId.
		 * @param groupId the group ID of the exclusion
		 * @param artifactId the artifact ID of the exclusion
		 */
		public Exclusion(String groupId, String artifactId) {
			this.groupId = groupId;
			this.artifactId = artifactId;
		}

		/**
		 * Returns the group ID of the Exclusion.
		 * @return the group ID of the Exclusion
		 */
		public String getGroupId() {
			return this.groupId;
		}

		/**
		 * Returns the artifact ID of the Exclusion.
		 * @return the artifact ID of the Exclusion
		 */
		public String getArtifactId() {
			return this.artifactId;
		}

	}

	/**
	 * Version alignment for a library.
	 */
	public static class VersionAlignment {

		private final String from;

		private final String managedBy;

		private final Project project;

		private final List<Library> libraries;

		private final List<Group> groups;

		private Set<String> alignedVersions;

		/**
		 * Initializes a new instance of the VersionAlignment class.
		 * @param from The source version from which the alignment is being performed.
		 * @param managedBy The entity responsible for managing the alignment.
		 * @param project The project for which the alignment is being performed.
		 * @param libraries The list of libraries involved in the alignment.
		 * @param groups The list of groups involved in the alignment.
		 */
		VersionAlignment(String from, String managedBy, Project project, List<Library> libraries, List<Group> groups) {
			this.from = from;
			this.managedBy = managedBy;
			this.project = project;
			this.libraries = libraries;
			this.groups = groups;
		}

		/**
		 * Resolves the aligned versions of the libraries based on the managedBy property.
		 * @return a Set of Strings representing the aligned versions
		 * @throws IllegalStateException if the managedBy property is null or if the
		 * managing library is not found
		 */
		public Set<String> resolve() {
			if (this.managedBy == null) {
				throw new IllegalStateException("Version alignment without managedBy is not supported");
			}
			if (this.alignedVersions != null) {
				return this.alignedVersions;
			}
			Library managingLibrary = this.libraries.stream()
				.filter((candidate) -> this.managedBy.equals(candidate.getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Managing library '" + this.managedBy + "' not found."));
			Map<String, String> versions = resolveAligningDependencies(managingLibrary);
			Set<String> versionsInLibrary = new HashSet<>();
			for (Group group : this.groups) {
				for (Module module : group.getModules()) {
					String version = versions.get(group.getId() + ":" + module.getName());
					if (version != null) {
						versionsInLibrary.add(version);
					}
				}
				for (String plugin : group.getPlugins()) {
					String version = versions.get(group.getId() + ":" + plugin);
					if (version != null) {
						versionsInLibrary.add(version);
					}
				}
			}
			this.alignedVersions = versionsInLibrary;
			return this.alignedVersions;
		}

		/**
		 * Resolves aligning dependencies using the given library manager.
		 * @param manager the library manager to use for resolving dependencies
		 * @return a map containing the resolved versions of the dependencies
		 */
		private Map<String, String> resolveAligningDependencies(Library manager) {
			DependencyHandler dependencyHandler = this.project.getDependencies();
			List<Dependency> boms = manager.getGroups()
				.stream()
				.flatMap((group) -> group.getBoms()
					.stream()
					.map((bom) -> dependencyHandler
						.platform(group.getId() + ":" + bom + ":" + manager.getVersion().getVersion())))
				.toList();
			List<Dependency> dependencies = new ArrayList<>();
			dependencies.addAll(boms);
			dependencies.add(dependencyHandler.create(this.from));
			Configuration alignmentConfiguration = this.project.getConfigurations()
				.detachedConfiguration(dependencies.toArray(new Dependency[0]));
			Map<String, String> versions = new HashMap<>();
			for (DependencyResult dependency : alignmentConfiguration.getIncoming()
				.getResolutionResult()
				.getAllDependencies()) {
				versions.put(dependency.getFrom().getModuleVersion().getModule().toString(),
						dependency.getFrom().getModuleVersion().getVersion());
			}
			return versions;
		}

		/**
		 * Returns the value of the 'from' field.
		 * @return the value of the 'from' field
		 */
		String getFrom() {
			return this.from;
		}

		/**
		 * Returns the value of the managedBy property.
		 * @return the value of the managedBy property
		 */
		String getManagedBy() {
			return this.managedBy;
		}

		/**
		 * Returns a string representation of the VersionAlignment object. The string
		 * includes the version from dependencies and the manager responsible for managing
		 * it.
		 * @return a string representation of the VersionAlignment object
		 */
		@Override
		public String toString() {
			return "version from dependencies of " + this.from + " that is managed by " + this.managedBy;
		}

	}

}
