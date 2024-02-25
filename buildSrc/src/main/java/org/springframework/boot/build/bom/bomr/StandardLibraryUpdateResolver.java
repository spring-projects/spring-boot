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

package org.springframework.boot.build.bom.bomr;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiPredicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * Standard implementation for {@link LibraryUpdateResolver}.
 *
 * @author Andy Wilkinson
 */
class StandardLibraryUpdateResolver implements LibraryUpdateResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(StandardLibraryUpdateResolver.class);

	private final VersionResolver versionResolver;

	private final BiPredicate<Library, DependencyVersion> predicate;

	/**
     * Constructs a new StandardLibraryUpdateResolver with the given VersionResolver and predicates.
     * 
     * @param versionResolver the VersionResolver used to resolve library versions
     * @param predicates a list of predicates used to filter libraries based on their versions
     */
    StandardLibraryUpdateResolver(VersionResolver versionResolver,
			List<BiPredicate<Library, DependencyVersion>> predicates) {
		this.versionResolver = versionResolver;
		this.predicate = (library, dependencyVersion) -> predicates.stream()
			.allMatch((predicate) -> predicate.test(library, dependencyVersion));
	}

	/**
     * Finds library updates for a collection of libraries to upgrade.
     * 
     * @param librariesToUpgrade the collection of libraries to upgrade
     * @param librariesByName the map of libraries by name
     * @return a list of LibraryWithVersionOptions objects representing the library updates
     */
    @Override
	public List<LibraryWithVersionOptions> findLibraryUpdates(Collection<Library> librariesToUpgrade,
			Map<String, Library> librariesByName) {
		List<LibraryWithVersionOptions> result = new ArrayList<>();
		for (Library library : librariesToUpgrade) {
			if (isLibraryExcluded(library)) {
				continue;
			}
			LOGGER.info("Looking for updates for {}", library.getName());
			long start = System.nanoTime();
			List<VersionOption> versionOptions = getVersionOptions(library);
			result.add(new LibraryWithVersionOptions(library, versionOptions));
			LOGGER.info("Found {} updates for {}, took {}", versionOptions.size(), library.getName(),
					Duration.ofNanos(System.nanoTime() - start));
		}
		return result;
	}

	/**
     * Checks if a library is excluded.
     * 
     * @param library the library to check
     * @return true if the library is excluded, false otherwise
     */
    protected boolean isLibraryExcluded(Library library) {
		return library.getName().equals("Spring Boot");
	}

	/**
     * Returns a list of version options for the given library.
     * 
     * @param library the library for which to determine the version options
     * @return a list of version options
     */
    protected List<VersionOption> getVersionOptions(Library library) {
		VersionOption option = determineAlignedVersionOption(library);
		return (option != null) ? List.of(option) : determineResolvedVersionOptions(library);
	}

	/**
     * Determines the aligned version option for a given library.
     * 
     * @param library the library for which to determine the aligned version option
     * @return the aligned version option, or null if no aligned version option is found
     */
    private VersionOption determineAlignedVersionOption(Library library) {
		VersionAlignment versionAlignment = library.getVersionAlignment();
		if (versionAlignment != null) {
			Set<String> alignedVersions = versionAlignment.resolve();
			if (alignedVersions != null && alignedVersions.size() == 1) {
				DependencyVersion alignedVersion = DependencyVersion.parse(alignedVersions.iterator().next());
				if (!alignedVersion.equals(library.getVersion().getVersion())) {
					return new VersionOption.AlignedVersionOption(alignedVersion, versionAlignment);
				}
			}
		}
		return null;
	}

	/**
     * Determines the resolved version options for a given library.
     * 
     * @param library the library for which to determine the resolved version options
     * @return a list of resolved version options
     */
    private List<VersionOption> determineResolvedVersionOptions(Library library) {
		Map<String, SortedSet<DependencyVersion>> moduleVersions = new LinkedHashMap<>();
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				moduleVersions.put(group.getId() + ":" + module.getName(),
						getLaterVersionsForModule(group.getId(), module.getName(), library));
			}
			for (String bom : group.getBoms()) {
				moduleVersions.put(group.getId() + ":" + bom, getLaterVersionsForModule(group.getId(), bom, library));
			}
			for (String plugin : group.getPlugins()) {
				moduleVersions.put(group.getId() + ":" + plugin,
						getLaterVersionsForModule(group.getId(), plugin, library));
			}
		}
		return moduleVersions.values()
			.stream()
			.flatMap(SortedSet::stream)
			.distinct()
			.filter((dependencyVersion) -> this.predicate.test(library, dependencyVersion))
			.map((version) -> (VersionOption) new VersionOption.ResolvedVersionOption(version,
					getMissingModules(moduleVersions, version)))
			.toList();
	}

	/**
     * Returns a list of missing modules based on the given module versions and dependency version.
     * 
     * @param moduleVersions a map containing module names as keys and sorted sets of dependency versions as values
     * @param version the dependency version to check against
     * @return a list of module names that do not contain the specified dependency version
     */
    private List<String> getMissingModules(Map<String, SortedSet<DependencyVersion>> moduleVersions,
			DependencyVersion version) {
		List<String> missingModules = new ArrayList<>();
		moduleVersions.forEach((name, versions) -> {
			if (!versions.contains(version)) {
				missingModules.add(name);
			}
		});
		return missingModules;
	}

	/**
     * Retrieves a sorted set of later versions for a given module.
     * 
     * @param groupId    the group ID of the module
     * @param artifactId the artifact ID of the module
     * @param library    the library object representing the module
     * @return a sorted set of DependencyVersion objects representing the later versions of the module
     */
    private SortedSet<DependencyVersion> getLaterVersionsForModule(String groupId, String artifactId, Library library) {
		return this.versionResolver.resolveVersions(groupId, artifactId);
	}

}
