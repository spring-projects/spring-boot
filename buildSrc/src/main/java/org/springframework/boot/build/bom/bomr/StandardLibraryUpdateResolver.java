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

package org.springframework.boot.build.bom.bomr;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.BiPredicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;
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

	StandardLibraryUpdateResolver(VersionResolver versionResolver,
			List<BiPredicate<Library, DependencyVersion>> predicates) {
		this.versionResolver = versionResolver;
		this.predicate = (library, dependencyVersion) -> predicates.stream()
			.allMatch((predicate) -> predicate.test(library, dependencyVersion));
	}

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
			List<VersionOption> versionOptions = getVersionOptions(library, librariesByName);
			result.add(new LibraryWithVersionOptions(library, versionOptions));
			LOGGER.info("Found {} updates for {}, took {}", versionOptions.size(), library.getName(),
					Duration.ofNanos(System.nanoTime() - start));
		}
		return result;
	}

	protected boolean isLibraryExcluded(Library library) {
		return library.getName().equals("Spring Boot");
	}

	protected List<VersionOption> getVersionOptions(Library library, Map<String, Library> libraries) {
		return determineResolvedVersionOptions(library);
	}

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

	private SortedSet<DependencyVersion> getLaterVersionsForModule(String groupId, String artifactId, Library library) {
		return this.versionResolver.resolveVersions(groupId, artifactId);
	}

}
