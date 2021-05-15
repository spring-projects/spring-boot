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

package org.springframework.boot.build.bom;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.versioning.VersionRange;

import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * A collection of modules, Maven plugins, and Maven boms that are versioned and released
 * together.
 *
 * @author Andy Wilkinson
 */
public class Library {

	private final String name;

	private final DependencyVersion version;

	private final List<Group> groups;

	private final String versionProperty;

	private final List<ProhibitedVersion> prohibitedVersions;

	/**
	 * Create a new {@code Library} with the given {@code name}, {@code version}, and
	 * {@code groups}.
	 * @param name name of the library
	 * @param version version of the library
	 * @param groups groups in the library
	 * @param prohibitedVersions version of the library that are prohibited
	 */
	public Library(String name, DependencyVersion version, List<Group> groups,
			List<ProhibitedVersion> prohibitedVersions) {
		this.name = name;
		this.version = version;
		this.groups = groups;
		this.versionProperty = "Spring Boot".equals(name) ? null
				: name.toLowerCase(Locale.ENGLISH).replace(' ', '-') + ".version";
		this.prohibitedVersions = prohibitedVersions;
	}

	public String getName() {
		return this.name;
	}

	public DependencyVersion getVersion() {
		return this.version;
	}

	public List<Group> getGroups() {
		return this.groups;
	}

	public String getVersionProperty() {
		return this.versionProperty;
	}

	public List<ProhibitedVersion> getProhibitedVersions() {
		return this.prohibitedVersions;
	}

	/**
	 * A version or range of versions that are prohibited from being used in a bom.
	 */
	public static class ProhibitedVersion {

		private final VersionRange range;

		private final String reason;

		public ProhibitedVersion(VersionRange range, String reason) {
			this.range = range;
			this.reason = reason;
		}

		public VersionRange getRange() {
			return this.range;
		}

		public String getReason() {
			return this.reason;
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

		public Group(String id, List<Module> modules, List<String> plugins, List<String> boms) {
			this.id = id;
			this.modules = modules;
			this.plugins = plugins;
			this.boms = boms;
		}

		public String getId() {
			return this.id;
		}

		public List<Module> getModules() {
			return this.modules;
		}

		public List<String> getPlugins() {
			return this.plugins;
		}

		public List<String> getBoms() {
			return this.boms;
		}

	}

	/**
	 * A module in a group.
	 */
	public static class Module {

		private final String name;

		private final List<Exclusion> exclusions;

		public Module(String name) {
			this(name, Collections.emptyList());
		}

		public Module(String name, List<Exclusion> exclusions) {
			this.name = name;
			this.exclusions = exclusions;
		}

		public String getName() {
			return this.name;
		}

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

		public Exclusion(String groupId, String artifactId) {
			this.groupId = groupId;
			this.artifactId = artifactId;
		}

		public String getGroupId() {
			return this.groupId;
		}

		public String getArtifactId() {
			return this.artifactId;
		}

	}

}
