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

package org.springframework.boot.build.bom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.GradleException;

import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * A collection of modules, Maven plugins, and Maven boms that are versioned and released
 * together.
 *
 * @author Andy Wilkinson
 */
public class Library {

	private final String name;

	private final LibraryVersion version;

	private final List<Group> groups;

	private final String versionProperty;

	private final List<ProhibitedVersion> prohibitedVersions;

	private final DependencyVersions dependencyVersions;

	/**
	 * Create a new {@code Library} with the given {@code name}, {@code version}, and
	 * {@code groups}.
	 * @param name name of the library
	 * @param version version of the library
	 * @param groups groups in the library
	 * @param prohibitedVersions version of the library that are prohibited
	 * @param dependencyVersions the library's dependency versions
	 */
	public Library(String name, LibraryVersion version, List<Group> groups, List<ProhibitedVersion> prohibitedVersions,
			DependencyVersions dependencyVersions) {
		this.name = name;
		this.version = version;
		this.groups = groups;
		this.versionProperty = "Spring Boot".equals(name) ? null
				: name.toLowerCase(Locale.ENGLISH).replace(' ', '-') + ".version";
		this.prohibitedVersions = prohibitedVersions;
		this.dependencyVersions = dependencyVersions;
	}

	public String getName() {
		return this.name;
	}

	public LibraryVersion getVersion() {
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

	public DependencyVersions getDependencyVersions() {
		return this.dependencyVersions;
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

	public static class LibraryVersion {

		private final DependencyVersion version;

		private final VersionAlignment versionAlignment;

		public LibraryVersion(DependencyVersion version, VersionAlignment versionAlignment) {
			this.version = version;
			this.versionAlignment = versionAlignment;
		}

		public DependencyVersion getVersion() {
			return this.version;
		}

		public VersionAlignment getVersionAlignment() {
			return this.versionAlignment;
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

		private final String type;

		private final List<Exclusion> exclusions;

		public Module(String name) {
			this(name, Collections.emptyList());
		}

		public Module(String name, String type) {
			this(name, type, Collections.emptyList());
		}

		public Module(String name, List<Exclusion> exclusions) {
			this(name, null, exclusions);
		}

		public Module(String name, String type, List<Exclusion> exclusions) {
			this.name = name;
			this.type = type;
			this.exclusions = exclusions;
		}

		public String getName() {
			return this.name;
		}

		public String getType() {
			return this.type;
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

	public interface DependencyVersions {

		String getVersion(String groupId, String artifactId);

		default boolean available() {
			return true;
		}

	}

	public static class DependencyLockDependencyVersions implements DependencyVersions {

		private final Map<String, Map<String, String>> dependencyVersions = new HashMap<>();

		private final String sourceTemplate;

		private final String libraryVersion;

		public DependencyLockDependencyVersions(String sourceTemplate, String libraryVersion) {
			this.sourceTemplate = sourceTemplate;
			this.libraryVersion = libraryVersion;
		}

		@Override
		public boolean available() {
			return !this.libraryVersion.contains("-SNAPSHOT");
		}

		@Override
		public String getVersion(String groupId, String artifactId) {
			if (this.dependencyVersions.isEmpty()) {
				loadVersions();
			}
			return this.dependencyVersions.computeIfAbsent(groupId, (key) -> Collections.emptyMap()).get(artifactId);
		}

		private void loadVersions() {
			String source = this.sourceTemplate.replace("<libraryVersion>", this.libraryVersion);
			try {
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(URI.create(source).toURL().openStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (!line.startsWith("#")) {
							String[] components = line.split(":");
							Map<String, String> groupDependencies = this.dependencyVersions
									.computeIfAbsent(components[0], (key) -> new HashMap<>());
							groupDependencies.put(components[1], components[2]);
						}
					}
				}
			}
			catch (IOException ex) {
				throw new GradleException("Failed to load versions from dependency lock file '" + source + "'", ex);
			}
		}

	}

	public static class DependencyConstraintsDependencyVersions implements DependencyVersions {

		private static final Pattern CONSTRAINT_PATTERN = Pattern.compile("api \"(.+):(.+):(.+)\"");

		private final Map<String, Map<String, String>> dependencyVersions = new HashMap<>();

		private final String sourceTemplate;

		private final String libraryVersion;

		public DependencyConstraintsDependencyVersions(String sourceTemplate, String libraryVersion) {
			this.sourceTemplate = sourceTemplate;
			this.libraryVersion = libraryVersion;
		}

		@Override
		public String getVersion(String groupId, String artifactId) {
			if (this.dependencyVersions.isEmpty()) {
				loadVersions();
			}
			return this.dependencyVersions.computeIfAbsent(groupId, (key) -> Collections.emptyMap()).get(artifactId);
		}

		private void loadVersions() {
			String version = this.libraryVersion;
			if (version.endsWith("-SNAPSHOT")) {
				version = version.substring(0, version.lastIndexOf('.')) + ".x";
			}
			String source = this.sourceTemplate.replace("<libraryVersion>", version);
			try {
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(URI.create(source).toURL().openStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						Matcher matcher = CONSTRAINT_PATTERN.matcher(line.trim());
						if (matcher.matches()) {
							Map<String, String> groupDependencies = this.dependencyVersions
									.computeIfAbsent(matcher.group(1), (key) -> new HashMap<>());
							groupDependencies.put(matcher.group(2), matcher.group(3));
						}
					}
				}
			}
			catch (IOException ex) {
				throw new GradleException(
						"Failed to load versions from dependency constraints declared in '" + source + "'", ex);
			}
		}

	}

	public static class VersionAlignment {

		private final String libraryName;

		public VersionAlignment(String libraryName) {
			this.libraryName = libraryName;
		}

		public String getLibraryName() {
			return this.libraryName;
		}

	}

}
