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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
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

	private final String alignsWithBom;

	private final String linkRootName;

	private final Map<String, List<Link>> links;

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
	 * @param alignsWithBom the coordinates of the bom, if any, that this library should
	 * align with
	 * @param linkRootName the root name to use when generating link variable or
	 * {@code null} to generate one based on the library {@code name}
	 * @param links a list of HTTP links relevant to the library
	 */
	public Library(String name, String calendarName, LibraryVersion version, List<Group> groups,
			List<ProhibitedVersion> prohibitedVersions, boolean considerSnapshots, VersionAlignment versionAlignment,
			String alignsWithBom, String linkRootName, Map<String, List<Link>> links) {
		this.name = name;
		this.calendarName = (calendarName != null) ? calendarName : name;
		this.version = version;
		this.groups = groups;
		this.versionProperty = "Spring Boot".equals(name) ? null
				: name.toLowerCase(Locale.ENGLISH).replace(' ', '-') + ".version";
		this.prohibitedVersions = prohibitedVersions;
		this.considerSnapshots = considerSnapshots;
		this.versionAlignment = versionAlignment;
		this.alignsWithBom = alignsWithBom;
		this.linkRootName = (linkRootName != null) ? linkRootName : generateLinkRootName(name);
		this.links = (links != null) ? Collections.unmodifiableMap(new TreeMap<>(links)) : Collections.emptyMap();
	}

	private static String generateLinkRootName(String name) {
		return name.replace("-", "").replace(" ", "-").toLowerCase(Locale.ROOT);
	}

	public String getName() {
		return this.name;
	}

	public String getCalendarName() {
		return this.calendarName;
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

	public boolean isConsiderSnapshots() {
		return this.considerSnapshots;
	}

	public VersionAlignment getVersionAlignment() {
		return this.versionAlignment;
	}

	public String getLinkRootName() {
		return this.linkRootName;
	}

	public String getAlignsWithBom() {
		return this.alignsWithBom;
	}

	public Map<String, List<Link>> getLinks() {
		return this.links;
	}

	public String getLinkUrl(String name) {
		List<Link> links = getLinks(name);
		if (links == null || links.isEmpty()) {
			return null;
		}
		if (links.size() > 1) {
			throw new IllegalStateException("Expected a single '%s' link for %s".formatted(name, getName()));
		}
		return links.get(0).url(this);
	}

	public List<Link> getLinks(String name) {
		return this.links.get(name);
	}

	public String getNameAndVersion() {
		return getName() + " " + getVersion();
	}

	public Library withVersion(LibraryVersion version) {
		return new Library(this.name, this.calendarName, version, this.groups, this.prohibitedVersions,
				this.considerSnapshots, this.versionAlignment, this.alignsWithBom, this.linkRootName, this.links);
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

		public ProhibitedVersion(VersionRange range, List<String> startsWith, List<String> endsWith,
				List<String> contains, String reason) {
			this.range = range;
			this.startsWith = startsWith;
			this.endsWith = endsWith;
			this.contains = contains;
			this.reason = reason;
		}

		public VersionRange getRange() {
			return this.range;
		}

		public List<String> getStartsWith() {
			return this.startsWith;
		}

		public List<String> getEndsWith() {
			return this.endsWith;
		}

		public List<String> getContains() {
			return this.contains;
		}

		public String getReason() {
			return this.reason;
		}

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

	public static class LibraryVersion {

		private final DependencyVersion version;

		public LibraryVersion(DependencyVersion version) {
			this.version = version;
		}

		public DependencyVersion getVersion() {
			return this.version;
		}

		public int[] componentInts() {
			return Arrays.stream(parts()).mapToInt(Integer::parseInt).toArray();
		}

		public String major() {
			return parts()[0];
		}

		public String minor() {
			return parts()[1];
		}

		public String patch() {
			return parts()[2];
		}

		@Override
		public String toString() {
			return this.version.toString();
		}

		public String toString(String separator) {
			return this.version.toString().replace(".", separator);
		}

		public String forAntora() {
			String[] parts = parts();
			String result = parts[0] + "." + parts[1];
			if (toString().endsWith("SNAPSHOT")) {
				result += "-SNAPSHOT";
			}
			return result;
		}

		public String forMajorMinorGeneration() {
			String[] parts = parts();
			String result = parts[0] + "." + parts[1] + ".x";
			if (toString().endsWith("SNAPSHOT")) {
				result += "-SNAPSHOT";
			}
			return result;
		}

		private String[] parts() {
			return toString().split("[.-]");
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

		private final String classifier;

		private final List<Exclusion> exclusions;

		public Module(String name) {
			this(name, Collections.emptyList());
		}

		public Module(String name, String type) {
			this(name, type, null, Collections.emptyList());
		}

		public Module(String name, List<Exclusion> exclusions) {
			this(name, null, null, exclusions);
		}

		public Module(String name, String type, String classifier, List<Exclusion> exclusions) {
			this.name = name;
			this.type = type;
			this.classifier = (classifier != null) ? classifier : "";
			this.exclusions = exclusions;
		}

		public String getName() {
			return this.name;
		}

		public String getClassifier() {
			return this.classifier;
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

		VersionAlignment(String from, String managedBy, Project project, List<Library> libraries, List<Group> groups) {
			this.from = from;
			this.managedBy = managedBy;
			this.project = project;
			this.libraries = libraries;
			this.groups = groups;
		}

		public Set<String> resolve() {
			if (this.alignedVersions != null) {
				return this.alignedVersions;
			}
			Map<String, String> versions = resolveAligningDependencies();
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

		private Map<String, String> resolveAligningDependencies() {
			List<Dependency> dependencies = getAligningDependencies();
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

		private List<Dependency> getAligningDependencies() {
			if (this.managedBy == null) {
				Library fromLibrary = findFromLibrary();
				return List
					.of(this.project.getDependencies().create(this.from + ":" + fromLibrary.getVersion().getVersion()));
			}
			else {
				Library managingLibrary = findManagingLibrary();
				List<Dependency> boms = getBomDependencies(managingLibrary);
				List<Dependency> dependencies = new ArrayList<>();
				dependencies.addAll(boms);
				dependencies.add(this.project.getDependencies().create(this.from));
				return dependencies;
			}
		}

		private Library findFromLibrary() {
			for (Library library : this.libraries) {
				for (Group group : library.getGroups()) {
					for (Module module : group.getModules()) {
						if (this.from.equals(group.getId() + ":" + module.getName())) {
							return library;
						}
					}
				}
			}
			return null;
		}

		private Library findManagingLibrary() {
			if (this.managedBy == null) {
				return null;
			}
			return this.libraries.stream()
				.filter((candidate) -> this.managedBy.equals(candidate.getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Managing library '" + this.managedBy + "' not found."));
		}

		private List<Dependency> getBomDependencies(Library manager) {
			if (manager == null) {
				return Collections.emptyList();
			}
			return manager.getGroups()
				.stream()
				.flatMap((group) -> group.getBoms()
					.stream()
					.map((bom) -> this.project.getDependencies()
						.platform(group.getId() + ":" + bom + ":" + manager.getVersion().getVersion())))
				.toList();
		}

		String getFrom() {
			return this.from;
		}

		String getManagedBy() {
			return this.managedBy;
		}

		@Override
		public String toString() {
			String result = "version from dependencies of " + this.from;
			if (this.managedBy != null) {
				result += " that is managed by " + this.managedBy;
			}
			return result;
		}

	}

	public record Link(String rootName, Function<LibraryVersion, String> factory, List<String> packages) {

		private static final Pattern PACKAGE_EXPAND = Pattern.compile("^(.*)\\[(.*)\\]$");

		public Link {
			packages = (packages != null) ? List.copyOf(expandPackages(packages)) : Collections.emptyList();
		}

		private static List<String> expandPackages(List<String> packages) {
			return packages.stream().flatMap(Link::expandPackage).toList();
		}

		private static Stream<String> expandPackage(String packageName) {
			Matcher matcher = PACKAGE_EXPAND.matcher(packageName);
			if (!matcher.matches()) {
				return Stream.of(packageName);
			}
			String root = matcher.group(1);
			String[] suffixes = matcher.group(2).split("\\|");
			return Stream.of(suffixes).map((suffix) -> root + suffix);
		}

		public String url(Library library) {
			return url(library.getVersion());
		}

		public String url(LibraryVersion libraryVersion) {
			return factory().apply(libraryVersion);
		}

	}

}
